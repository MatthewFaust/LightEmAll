


https://github.com/user-attachments/assets/fffde5c1-fb7d-4541-b967-239ed64438d1







The provided Java code outlines a game named "LightEmAll," a grid-based puzzle game. Here's a breakdown of the key components and functionality:

### Game Overview:
- **Game Objective**: Connect all game pieces to a central power station by rotating pieces to align wires. The game uses a grid where each cell (GamePiece) has various states and connections.
  
### Main Classes:

1. **`GamePiece`**:
   - Represents individual pieces in the game grid.
   - Attributes:
     - `row`, `col`: Position on the grid.
     - `left`, `right`, `top`, `bottom`: Boolean values indicating connections to adjacent pieces.
     - `powerStation`, `powered`: States indicating if the piece is a power station or is powered.
   - Methods:
     - `tileImage()`: Generates the visual representation of the piece based on its state and connections.
     - `colorFinder()`: Determines the color of the piece based on its distance from the power station.
     - `rotater()`: Rotates the piece to change its connections.
     - `distanceFromPowerStation()`: Calculates Manhattan distance from the power station.

2. **`LightEmAll`**:
   - Manages the game world and logic.
   - Attributes:
     - `board`: 2D list representing the grid of game pieces.
     - `nodes`, `edges`, `mst`: Lists for game pieces, edges, and minimum spanning tree.
     - `powerRow`, `powerCol`: Coordinates of the power station.
     - `radius`, `score`, `tickCount`: Game state variables.
   - Methods:
     - `createPieces()`, `createEdges()`, `createPuzzle()`: Initialize the board, edges, and create the puzzle using Kruskal’s algorithm.
     - `makeCorrectBoard()`: Saves the correct solution for comparison.
     - `rotate()`: Randomly rotates game pieces.
     - `connect()`: Updates the powered state of game pieces.
     - `makeScene()`: Constructs the visual representation of the game world.
     - `onKeyEvent()`, `onMouseClicked()`, `onTick()`: Handle user input and game updates.
     - `calculateMaxDistance()`: Computes the maximum distance from the power station to any piece.

3. **`Edge`**:
   - Represents a connection between two game pieces with a randomly assigned weight.
   - Attributes:
     - `fromNode`, `toNode`: Connected game pieces.
     - `weight`: Weight of the edge, used in the puzzle creation algorithm.

### Summary:
The code sets up a game environment where players rotate pieces to connect all game pieces to a central power station. It involves generating a grid-based puzzle, managing game states, handling user interactions, and visualizing the game world.
