import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//class to represent a GamePiece
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;

  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  //constructor for gamepiece
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom) {
    this(row, col, left, right, top, bottom, false, false);
  }

  //constructor for gamepiece with only row and col
  GamePiece(int row, int col) {
    this(row, col, false, false, false, false, false, false);
  }

  //constructor for gamepiece
  GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom, 
      boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;
  }

  //produces a tile based off the given tiles
  public WorldImage tileImage(int size, int wireWidth, int powerRow, int powerCol, int maxDistance, 
      boolean hasPowerStation) {
    int distance = this.distanceFromPowerStation(powerRow, powerCol);
    Color wireColor = this.colorFinder(distance, maxDistance);
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, 
          AlignModeY.TOP, vWire, 0, 0, image);
    }

    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, 
          AlignModeY.MIDDLE, hWire, 0, 0, image);
    }

    if (this.bottom) { 
      image = new OverlayOffsetAlign(AlignModeX.CENTER, 
          AlignModeY.BOTTOM, vWire, 0, 0, image);
    }

    if (this.left) { 
      image = new OverlayOffsetAlign(AlignModeX.LEFT, 
          AlignModeY.MIDDLE, hWire, 0, 0, image);
    }

    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(
              new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }

  //return the color according to how far it is from the powerStation
  public Color colorFinder(int distance, int maxDistance) {
    if (this.powered) {
      float ratio = distance / (float) maxDistance;
      int red = Math.min(255, (int) (255 * ratio));
      int green = Math.max(0, 255 - (int) (255 * ratio));
      return new Color(red, green, 0);
    } else {
      return Color.LIGHT_GRAY;
    }
  }

  //rotates cell according to num
  public void rotater(int num) {
    for (int i = 0; i < num; i++)  {

      boolean directionTop = this.top;
      this.top = this.left;
      this.left = this.bottom;
      this.bottom = this.right;
      this.right = directionTop;

    }
  }

  //calculates the distance a wire is from the powerstation
  public int distanceFromPowerStation(int powerRow, int powerCol) {
    return Math.abs(this.row - powerRow) + Math.abs(this.col - powerCol);
  }

}

//class to represent LightEmAll
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  ArrayList<ArrayList<GamePiece>> completeBoard;

  // a list of all nodes
  ArrayList<GamePiece> nodes;

  //a list of all edges
  ArrayList<Edge> edges;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;

  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;

  //for testing
  Random rand;

  //to see if game is over
  boolean gameEnd;

  //to keep track of the score
  int score;

  //to keep track of the tickCount
  int tickCount;

  //constructor for testing
  LightEmAll(int width, int height, Random rand) {
    this(new ArrayList<ArrayList<GamePiece>>(),new ArrayList<GamePiece>(), 
        width, height, 0, 0, 0, new Random());
  }

  //constructor with everything
  LightEmAll( ArrayList<ArrayList<GamePiece>> board, ArrayList<GamePiece> nodes,
      int width, int height, int powerRow, int powerCol, int radius, Random rand) {
    this.board = new ArrayList<ArrayList<GamePiece>>();
    this.nodes = new ArrayList<GamePiece>();
    this.width = width;
    this.height = height;
    this.powerRow = powerRow;
    this.powerCol = powerCol;
    this.radius = radius;
    this.rand = new Random();
    this.gameEnd = false;
    this.score = 0;
    this.completeBoard = new ArrayList<ArrayList<GamePiece>>();

    // initializes list of edges
    edges = new ArrayList<Edge>();
    // initializes the solutiuon of the board
    mst = new ArrayList<Edge>();

    //creates the board
    this.createPieces();

    //creates all the edges 
    this.createEdges();

    //creates a unique puzzle for the board 
    this.createPuzzle();

    //modifies each edge to have the correct direction
    this.edgeDirection();

    //saves the correct board to be shown later
    this.makeCorrectBoard();

    //rotate every piece when the game is first generated
    this.rotate();

    //tracks the amount of time elapsed
    this.onTick();
  }


  //saves the correct board to be shown later
  void makeCorrectBoard() {

    for (int i = 0; i < height; i++) {

      completeBoard.add(new ArrayList<GamePiece>());

      for (int j = 0; j < width; j++) {

        GamePiece correctPiece = new GamePiece(i, j, false, false, false, false, false, false);
        correctPiece.powered = board.get(i).get(j).powered;
        correctPiece.powerStation = board.get(i).get(j).powerStation;
        correctPiece.left = board.get(i).get(j).left;
        correctPiece.right = board.get(i).get(j).right;
        correctPiece.top = board.get(i).get(j).top;
        correctPiece.bottom = board.get(i).get(j).bottom;


        completeBoard.get(i).add(correctPiece); 
      }
    }
  }

  //creates all the edges 
  void createEdges() {
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {

        if (j < width - 1) {
          edges.add(new Edge(board.get(i).get(j), board.get(i).get(j + 1)));
        }

        if (i < height - 1) {
          edges.add(new Edge(board.get(i).get(j), board.get(i + 1).get(j)));
        }
      }
    }
  }

  //creates the board 
  void createPieces() {
    for (int i = 0; i < height; i++) {
      board.add(new ArrayList<GamePiece>());
      for (int j = 0; j < width; j++) {
        GamePiece def = new GamePiece(i, j, false, false, false, false, false, false);
        board.get(i).add(def); 

        // places power station at the top left initially
        if (i == 0 && j == 0) {
          board.get(i).get(j).powered = true;
          board.get(i).get(j).powerStation = true;
          this.powerRow = i;
          this.powerCol = j;
        }
      }
    }
  }

  //class to compare the weights of two edges
  class CompareEdges implements Comparator<Edge> {

    //subtracts the weights of the two edges
    public int compare(Edge that, Edge other) {
      return that.weight - other.weight;
    }
  }

  //look for the GamePiece
  GamePiece search(HashMap<GamePiece, GamePiece> hashmap, GamePiece gp) {
    if (hashmap.get(gp).equals(gp)) {
      return gp;
    }
    else {
      return search(hashmap, hashmap.get(gp));
    }
  }

  //combine the two gamePieces
  void combine(HashMap<GamePiece, GamePiece> hash, GamePiece g1, GamePiece g2) {
    hash.put(g1, g2);
  }

  //modifies each edge to have the correct direction
  void edgeDirection() {
    for (Edge edge : this.mst) {
      if (edge.fromNode.row == edge.toNode.row) {


        edge.fromNode.right = true;
        edge.toNode.left = true;
      }
      else {
        edge.fromNode.bottom = true;
        edge.toNode.top = true;
      }
    }

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        nodes.add(board.get(i).get(j));
      }
    }
  }

  //creates the board with the solution
  void createPuzzle() {

    HashMap<GamePiece, GamePiece> rep = new HashMap<GamePiece, GamePiece>();
    this.edges.sort(new CompareEdges());

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        rep.put(board.get(i).get(j), board.get(i).get(j));
      }
    }

    //kruskal's algorithm
    while (this.edges.size() > 0) {
      Edge current = this.edges.get(0);

      if (search(rep, current.fromNode).equals(search(rep, current.toNode))) {
        this.edges.remove(0);
      }
      else {
        this.mst.add(current); 
        combine(rep, search(rep, current.fromNode), search(rep, current.toNode));
      }
    }
  }

  //rotate every piece randomly
  void rotate() {
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int counter = 0;
        while (counter < this.rand.nextInt(5)) {
          board.get(i).get(j).rotater(rand.nextInt(5));
          counter++;
        }
      }
    }
  }

  //checks if wires are connected to power station and makes them powered  
  public void connect() {
    // list of games pieces that need to be processed
    ArrayList<GamePiece> listOfPiece = new ArrayList<GamePiece>();

    // initializes game pieces as not powered 
    for (GamePiece allPiece : this.nodes) {
      allPiece.powered = false;
    }

    // adds power station to head of list
    listOfPiece.add(board.get(powerRow).get(powerCol));

    // while loop that checks surrounding cells for power up
    while (!listOfPiece.isEmpty()) {

      if (listOfPiece.get(0).top && listOfPiece.get(0).row > 0) {

        GamePiece piece = board.get((listOfPiece.get(0).row) - 1).get(listOfPiece.get(0).col);

        if (piece.bottom && !piece.powered) {
          piece.powered = true;
          listOfPiece.add(piece);
        }
      }

      if (listOfPiece.get(0).bottom && listOfPiece.get(0).row < this.height - 1) {

        GamePiece piece = board.get((listOfPiece.get(0).row) + 1).get(listOfPiece.get(0).col);

        if (piece.top && !piece.powered) {
          piece.powered = true;
          listOfPiece.add(piece);
        }
      }

      if (listOfPiece.get(0).left && listOfPiece.get(0).col > 0) {

        GamePiece piece = board.get(listOfPiece.get(0).row).get((listOfPiece.get(0).col) - 1);


        if (piece.right && !piece.powered) {
          piece.powered = true;
          listOfPiece.add(piece);
        }
      }


      if (listOfPiece.get(0).right && listOfPiece.get(0).col < this.width - 1) {

        GamePiece piece = board.get(listOfPiece.get(0).row).get((listOfPiece.get(0).col) + 1);

        if (piece.left  && !piece.powered) {
          piece.powered = true;
          listOfPiece.add(piece);
        }
      }

      // removes already processed game piece 
      listOfPiece.remove(0);
    }
  }

  // variables for makeScene
  int size = 40;
  int wireWidth = 4;

  //method to make the scene
  public WorldScene makeScene() {
    int maxDistance = calculateMaxDistance();  // Calculate maximum distance
    WorldScene scene = new WorldScene(width * size, height * size + 100);
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        GamePiece piece = board.get(i).get(j);
        scene.placeImageXY(piece.tileImage(size, wireWidth, powerRow, powerCol,
            maxDistance, board.get(i).get(j).powerStation),
            (j * size) + size / 2, (i * size) + size / 2);
      }
    }

    //placing the box where time and score are displayed
    scene.placeImageXY(new RectangleImage(width * size, 100, OutlineMode.SOLID,
        Color.DARK_GRAY), width * size / 2, height * size + 50);

    //placing the black line
    scene.placeImageXY(new RectangleImage(width * size, 1, OutlineMode.OUTLINE,
        Color.BLACK), width * size / 2, height * size + 1);


    //placing the score on the scene
    scene.placeImageXY(new TextImage("Your Score is: " + String.valueOf(score), 
        20, FontStyle.BOLD, Color.CYAN), this.width * this.size - 100, height * size + 50);


    //placing the time on the scene
    scene.placeImageXY(new TextImage("Time Elapsed: " + tickCount, 20,
        FontStyle.BOLD, Color.CYAN), 100, height * size + 50);

    this.connect();

    if (this.gameEnd) {
      scene.placeImageXY(new RectangleImage(width * 30, height * 12, 
          OutlineMode.OUTLINE, Color.ORANGE), width * 20, height * 20);
      TextImage endMessage = new TextImage("YOU WON! Your Score is: " + String.valueOf(score), 
          20, FontStyle.BOLD, Color.CYAN);
      scene.placeImageXY(endMessage, width * 20, height * 20);

      // Position the tick counter text on the scene, adjust coordinates as needed
      scene.placeImageXY(new TextImage("Time Elapsed: " + tickCount, 14,
          FontStyle.BOLD, Color.CYAN), width * 20, height * 22);
    }




    return scene;
  }


  //OnKeyEvent handler
  public void onKeyEvent(String key) {

    if (!this.gameEnd) {

      //only allows correct keys
      if (key.equals("left") || key.equals("right") || key.equals("up") || 
          key.equals("down") || key.equals("r") || key.equals("tab")) {

        if (board.get(powerRow).get(powerCol).powerStation) {

          //handles the left arrow key
          if (key.equals("left") && powerCol != 0 && board.get(powerRow)
              .get(powerCol).left && board.get(powerRow).get(powerCol - 1).right) {
            board.get(powerRow).get(powerCol).powerStation = false;
            board.get(powerRow).get(powerCol - 1).powerStation = true;
            board.get(powerRow).get(powerCol - 1).powered = true;
            powerCol -= 1;

            //handles the right arrow key
          } else if (key.equals("right") && powerCol != width - 1 && board.get(powerRow)
              .get(powerCol).right && board.get(powerRow).get(powerCol + 1).left) {
            board.get(powerRow).get(powerCol).powerStation = false;
            board.get(powerRow).get(powerCol + 1).powerStation = true;
            board.get(powerRow).get(powerCol + 1).powered = true;
            powerCol += 1;

            //handles the up arrow key
          } else if (key.equals("up") && powerRow != 0 && board.get(powerRow)
              .get(powerCol).top && board.get(powerRow - 1).get(powerCol).bottom) {
            board.get(powerRow).get(powerCol).powerStation = false;
            board.get(powerRow - 1).get(powerCol).powerStation = true;
            board.get(powerRow - 1).get(powerCol).powered = true;
            powerRow -= 1;


            //handles the down arrow key
          } else if (key.equals("down") && powerRow != height - 1 && board.get(powerRow)
              .get(powerCol).bottom && board.get(powerRow + 1).get(powerCol).top) {
            board.get(powerRow).get(powerCol).powerStation = false;
            board.get(powerRow + 1).get(powerCol).powerStation = true;
            board.get(powerRow + 1).get(powerCol).powered = true;
            powerRow += 1;

          }

          //create a game with new pieces whenever you click "r"
          else if (key.equals("r")) {
            this.rotate();
            //reset the score to 0
            this.score = 0;
            this.tickCount = 0;
          }

          //shows the solution to the game
          else if (key.equals("tab")) {
            this.board = this.completeBoard;
            this.gameEnd = true;
            //reset the score to zero
            this.score = 0;
            this.tickCount = 0;
          }
        }
      }
    }
  }

  //mouseClicked handler
  public void onMouseClicked(Posn pos, String key) {

    //checks if game is over otherwise rotates
    if (!this.gameEnd) {

      if ("LeftButton".equals(key)) {
        GamePiece piece = board.get((int)Math.floor(pos.y / 40))
            .get((int)Math.floor(pos.x / 40)); 
        piece.rotater(1);
        score += 1;
      }

      //updates the game over variable
      boolean gameState = true;
      for (int i = 0 ; i < height ; i++) {
        for (int j = 0; j < width ; j++) {
          if (!board.get(i).get(j).powered) {
            gameState = false;
          }
        }
      }
      if (gameState) {
        this.gameEnd = true;
      }
    }

  }

  //adds to the tickCount if the game has not ended
  public void onTick() {
    if (!gameEnd) {
      tickCount++; 
    }
  }

  //Method to calculate the maximum distance from the power station to any piece
  public int calculateMaxDistance() {
    int maxDist = 0;
    //check distance from the power station to all corners
    maxDist = Math.max(maxDist, board.get(0).get(0)
        .distanceFromPowerStation(powerRow, powerCol));
    maxDist = Math.max(maxDist, board.get(0).get(width - 1)
        .distanceFromPowerStation(powerRow, powerCol));
    maxDist = Math.max(maxDist, board.get(height - 1).get(0)
        .distanceFromPowerStation(powerRow, powerCol));
    maxDist = Math.max(maxDist, board.get(height - 1).get(width - 1)
        .distanceFromPowerStation(powerRow, powerCol));
    return maxDist;
  }

}

//class to represent an edge between two game pieces 
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;
  Random random; // random object to assign random weight

  //constructor for edge 
  Edge(GamePiece fromNode, GamePiece toNode) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    random = new Random();
    this.weight = random.nextInt(100);
  }
}


//examples class
class ExamplesLightEmAll {

  //random
  Random rand;

  //example of a game
  LightEmAll game1;

  //constructor for ExamplesLightEmAll
  ExamplesLightEmAll() {
    this(new Random());
  }


  //constructor for ExamplesLightEmALl
  ExamplesLightEmAll(Random rand) {
    this.rand = rand;
  }

  //the initial testing conditions
  void initTestConditions() {

    //example of a game
    game1 = new LightEmAll(10, 10, new Random(4));
  }

  //running the bigBang method
  void testBigBang(Tester t) {

    //initializing the test conditions
    this.initTestConditions();

    //running game1 on bigBang
    this.game1.bigBang(400, 500, 1);

  }

  //testing the onKeyEvent method
  void testOnKeyEvent(Tester t) {

    //initializing the test conditions
    this.initTestConditions();

    game1.connect();

    //checking that the powerStation is there
    t.checkExpect(game1.board.get(0).get(0).powerStation, true);

    //create a cross piece at the given index
    game1.board.get(0).get(0).bottom = true;
    game1.board.get(0).get(0).top = true;
    game1.board.get(0).get(0).left = true;
    game1.board.get(0).get(0).right = true;

    //create a cross piece at the given index
    game1.board.get(1).get(0).bottom = true;
    game1.board.get(1).get(0).top = true;
    game1.board.get(1).get(0).left = true;
    game1.board.get(1).get(0).right = true;

    //running onKeyEvent with the down string
    this.game1.onKeyEvent("down");

    //checking that the piece below now contains the powerStation
    t.checkExpect(game1.board.get(1).get(0).powerStation, true);

    //running onKeyEvent with the up string
    this.game1.onKeyEvent("up");

    //checking that the piece above now contains the powerStation
    t.checkExpect(game1.board.get(0).get(0).powerStation, true);

  }

  //testing the onMouseClicked method
  void testOnMouseClicked(Tester t) {

    //initializing the test conditions
    this.initTestConditions();

    //save the original piece in a local
    GamePiece originalPiece = game1.board.get(4).get(4);

    originalPiece.top = true;
    originalPiece.bottom = true;
    originalPiece.left = false;
    originalPiece.right = false;

    //check the original piece
    t.checkExpect(originalPiece.top, true);
    t.checkExpect(originalPiece.bottom, true);
    t.checkExpect(originalPiece.left, false);
    t.checkExpect(originalPiece.right, false);

    //modify the game piece
    game1.onMouseClicked(new Posn(160, 160), "LeftButton");

    //check that the expected changes have occurred
    t.checkExpect(originalPiece.top, false);
    t.checkExpect(originalPiece.bottom, false);
    t.checkExpect(originalPiece.left, true);
    t.checkExpect(originalPiece.right, true);
  }

  //testing the connect method
  void testConnect(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //get two game pieces
    GamePiece origin = this.game1.board.get(0).get(0);
    GamePiece neighbor = this.game1.board.get(0).get(1);

    //create the connection
    origin.right = true;
    neighbor.left = true;

    //set neighbor to unpowered initially
    neighbor.powered = false;

    //run the connect method
    this.game1.connect();

    //check that the expected changes have occurred
    t.checkExpect(origin.powered, true);
    t.checkExpect(neighbor.powered, true);
  }

  //testing the edgeDirection method
  void testEdgeDirection(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //get two pieces and make them an edge
    GamePiece gp1 = this.game1.board.get(0).get(0);
    GamePiece gp2 = this.game1.board.get(0).get(1);
    Edge edge = new Edge(gp1, gp2);

    //add the edge to the MST
    this.game1.mst.add(edge);

    //call the edgeDirection method
    this.game1.edgeDirection();

    //check if the expected changes have occurred
    t.checkExpect(gp1.right, true);
    t.checkExpect(gp2.left, true);
  }

  //testing the rotate method
  void testRotate(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //get a piece from the game board
    GamePiece originalPiece = this.game1.board.get(0).get(0);

    //create the original piece
    originalPiece.left = true;
    originalPiece.right = true;
    originalPiece.top = false;
    originalPiece.bottom = false;

    //rotate the piece 90 degrees
    originalPiece.rotater(1);

    //check that the expected changes have occurred
    t.checkExpect(originalPiece.top, true);
    t.checkExpect(originalPiece.bottom, true);
    t.checkExpect(originalPiece.left, false);
    t.checkExpect(originalPiece.right, false);
  }

  //testing the makeScene method
  void testMakeScene(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //calculate the maximum distance
    int maxDistance = game1.calculateMaxDistance();
    WorldScene scene = new WorldScene(game1.width * game1.size, game1.height * game1.size + 100);
    for (int i = 0; i < game1.height; i++) {
      for (int j = 0; j < game1.width; j++) {
        GamePiece piece = game1.board.get(i).get(j);
        scene.placeImageXY(piece.tileImage(game1.size, game1.wireWidth, game1.powerRow, 
            game1.powerCol, maxDistance, game1.board.get(i).get(j).powerStation),
            (j * game1.size) + game1.size / 2, (i * game1.size) + game1.size / 2);
      }
    }

    //placing the box where time and score are displayed
    scene.placeImageXY(new RectangleImage(game1.width * game1.size, 100, OutlineMode.SOLID,
        Color.DARK_GRAY), game1.width * game1.size / 2, game1.height * game1.size + 50);

    //placing the black line
    scene.placeImageXY(new RectangleImage(game1.width * game1.size, 1, OutlineMode.OUTLINE,
        Color.BLACK), game1.width * game1.size / 2, game1.height * game1.size + 1);


    //placing the score on the scene
    scene.placeImageXY(new TextImage("Your Score is: " + String.valueOf(game1.score), 20, 
        FontStyle.BOLD, Color.CYAN), 
        game1.width * game1.size - 100, game1.height * game1.size + 50);

    //placing the time on the scene
    scene.placeImageXY(new TextImage("Time Elapsed: " + game1.tickCount, 20,
        FontStyle.BOLD, Color.CYAN), 100, game1.height * game1.size + 50);

    //check that the expected changes have occurred
    t.checkExpect(this.game1.makeScene(), scene);
  }


  //testing the createPuzzle method
  void testCreatePuzzle(Tester t) {

    //creating a game example
    LightEmAll game = new LightEmAll(3, 3, new Random(42));
    game.createPieces();
    game.createEdges();

    //before running createPuzzle
    t.checkExpect(game.mst.size(), 8);

    //run the create puzzle method
    game.createPuzzle();

    //check that the expected changes have occurred
    t.checkExpect(game.mst.size(), 16);
  }

  //testing the createPieces method
  void testCreatePieces(Tester t) {

    //creating a game example
    LightEmAll game = new LightEmAll(3, 3, new Random());

    //call the createPieces method
    game.createPieces(); // Call the method to test

    //check that the expected changes have occurre
    t.checkExpect(game.board.size(), 6);

    //check that the powerStation is in the correct area
    GamePiece firstPiece = game.board.get(0).get(0);
    t.checkExpect(firstPiece.powerStation, true); 
    t.checkExpect(firstPiece.powered, true);
  }

  //testing the createEdges method
  void testCreateEdges(Tester t) {

    //creating a game example
    LightEmAll game = new LightEmAll(3, 3, new Random());
    game.createPieces();
    game.createEdges();

    //check the size of the edges
    t.checkExpect(game.edges.size(), 12);

    //check that the expected changes have occurred
    Edge firstHorizontalEdge = game.edges.get(0);
    t.checkExpect(firstHorizontalEdge.fromNode, game.board.get(0).get(0));
    t.checkExpect(firstHorizontalEdge.toNode, game.board.get(0).get(1));
  }

  //testing the search method
  void testSearch(Tester t) {

    //creating a game example
    LightEmAll game = new LightEmAll(2, 2, new Random());
    game.createPieces();

    //initialize a new hashMap
    HashMap<GamePiece, GamePiece> parentMap = new HashMap<>();

    //get four different pieces
    GamePiece gp00 = game.board.get(0).get(0);
    GamePiece gp01 = game.board.get(0).get(1);
    GamePiece gp10 = game.board.get(1).get(0);
    GamePiece gp11 = game.board.get(1).get(1);

    //put two pieces together
    parentMap.put(gp00, gp00);
    parentMap.put(gp01, gp00);
    parentMap.put(gp10, gp00);
    parentMap.put(gp11, gp10);

    //check that the expected changes have occurred
    t.checkExpect(game.search(parentMap, gp00), gp00);
    t.checkExpect(game.search(parentMap, gp01), gp00);
    t.checkExpect(game.search(parentMap, gp10), gp00);
    t.checkExpect(game.search(parentMap, gp11), gp00);
  }

  //testing the union method
  void testCombine(Tester t) {

    //create a game example
    LightEmAll game = new LightEmAll(2, 2, new Random());
    game.createPieces();

    //initialize a hashMap
    HashMap<GamePiece, GamePiece> parentMap = new HashMap<>();

    //get two pieces
    GamePiece gp00 = game.board.get(0).get(0);
    GamePiece gp01 = game.board.get(0).get(1);

    parentMap.put(gp00, gp00);
    parentMap.put(gp01, gp01);

    if (!game.search(parentMap, gp00).equals(game.search(parentMap, gp01))) {
      game.combine(parentMap, game.search(parentMap, gp00), game.search(parentMap, gp01));
    }

    //check that the expected changes have occurred
    GamePiece rootForGP00 = game.search(parentMap, gp00);
    GamePiece rootForGP01 = game.search(parentMap, gp01);
    t.checkExpect(rootForGP00, rootForGP01);
  }

  //testing the calculateMaxDistance method
  void testMaxDistance(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //check that the expected changes have occurred
    t.checkExpect(this.game1.calculateMaxDistance(), 18);
  }

  //testing the distanceFromPowerStation method
  void testDistanceFromPowerStation(Tester t) {

    //set up the initial conditions
    this.initTestConditions();

    //retrieve a piece from the board
    GamePiece piece = game1.board.get(1).get(4);

    //check that the expected changes have occurred
    t.checkExpect(piece.distanceFromPowerStation(5, 5), 5);
  }

}