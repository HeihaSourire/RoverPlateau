# Analyse #
1. Goal: get a complete view of the surrounding terrain
2. A squad of robotic rovers. Move sequentially.
3. rover's position : (x, y) and `E`, `W`, `S`, `N`
   1) ex: 0, 0, N: at (x, y) and face North.
4. input:
   1. Upper-right coordinates (maxX, maxY) of the plateau (lower-left is (0, 0))
   2. Each rover has two lines of input
      1. Rover's position
      2. a series of instructions: a string of letters
         1) Possible letters: 
            1) `L`: spin 90 degrees left
            2) `R`: spin 90 degrees right
            3) `M`: move forward on grid point, maintain the same heading
5. output: Each rover's final coordinates and heading
6. Execution: `java -jar rover.jar input.txt`

# Thinking #
## I/O: ##
   1. Input file：
      1. First Line: two integers. `<maxX> <maxY>`.
      2. Two lines for one rover:
         1. Position line: `<x> <y> <N|E|S|W>`; ex: 1 2 N.
         2. Instruction line: ex: LMLMLMLMM.
   2. Output:
      1. One line for one rover: `<x> <y> <heading>`.
## Error Handling ##
### I. Definition of risks and But: ###
* Risks: Out of Bound, Potential Collision (at the same position with preceding rover after `M`)
* Goal: Under of premise of maximising coverage (Complete View), ensure hardware security and task continuity (minimising
disruption to other rovers)

### Pre-check Phase (No Actual Rover Movement) ###
* Goal: To identify all issues comprehensively prior to actual use.
* Strategy A: Strict Pre-check
  * Upon detecting any out-of-bounds/collision: End and immediately flag with highlighting and provide precise location 
and cause (line number, step number, coordinates, orientation).
  * Output on the terminal (also write in the log): 
    * Issue list: error type, line number, affected rover, step number, final coordinates and orientation.
    * Coverage Report: covered cells / total cells
    * Time Estimate (optional)
* Strategy B: Auto-fix
  * Upon detecting any out-of-bounds/collision: don't move, but process followed instruction.
  * Alters original instructions, reducing interpretability of coverage paths

### Execution Phase (Actual Movement) ###
* Precondition: Assuming pre-check have passed
* Strategy A: Stop-this-rover (Fail-Fast per rover):
  * Current rover: Don't execute this step and terminate subsequent instructions. Mark status as Fault-Stopped.
  * Other rovers: Continue executing as planned
  * Logging: Record fault line number, fault rover, cause, step number (replanning entry point).
* Strategy B: Stop-Only-This-Step:
  * Don't execute this step but proceeding to subsequent instructions.
  * Unpredictable paths. Can not move to the target point.
* Strategy C: Abort Mission
  * Highest safety. No suitable for operation on actual hardware.
  
### Log ###
* Structured Event: Timestamp, RoverId, StepId, Pos(x, y), heading, Event{MOVE, TURN, FAULT_OOB, FAULT_COLLISION}
* MOVE, TURN: info
* FAULT_OOB, FAULT_COLLISION: warning or Error.

# Functional Requirements #
1. Spin:
   * `L`: `N→W→S→E→N`
   * `R`: `N→E→S→W→N`
2. `M`:
    * `N`: (x, y+1)
    * `E`: (x+1, y)
    * `S`: (x, y-1)
    * `W`: (x-1, y)
3. Case sensitivity: To enhance robustness, lowercase `l/r/m` and lowercase orientation `n/e/s/w` are acceptable
4. Allow blank lines and extra spaces (trimmed during parsing).
5. Save structured event in the log after every instruction.
6. Collision: Upon completion of the preceding rover, its final position shall be deemed occupied; following rovers shall not enter that position.
   If the initial coordinates of the rover match the final coordinates of any completed rovers → this shall be deemed a collision.
7. Execution: `java -jar rover.jar input.txt` `11>positions.txt 2>logs.ndjson` `[--mode=fail-fast|fail-fast-per-rover(default)|skip-step]`

## Running Model ##
| Model                         | Current Rover                                                | Following Rovers | Terminal Output                                                                                         | Logging                                | Exit Code                                        |
|-------------------------------|--------------------------------------------------------------|------------------|---------------------------------------------------------------------------------------------------------|----------------------------------------|--------------------------------------------------|
| fail-fast                     | end the entire program                                       | Not executed     | Error Message (error type, line number, affected rover, step number, final coordinates and orientation) | Standard: INFO; OOB/Collision: ERROR   | 0 if no OOB/Collision, 3 when with OOB/Collision |
| fail-fast-per-rover (Default) | Stop the Rover                                               | continue         | Final position of each rover                                                                            | Standard: INFO; OOB/Collision: WARNING | 0 when valid input                               |
| skip-step                     | Skip this step and continue executing following instructions | continue         | Final position of each rover                                                                            | Standard: INFO; OOB/Collision: WARNING | 0 when valid input                               |

## Exit Code ##
| Code | Scenario                                                                                               |
|------|--------------------------------------------------------------------------------------------------------|
| `0`  | Completed                                                                                              |
| `1`  | File I/O error (file does not exist, cannot be read, etc.)                                             |
| `2`  | Input format error (missing line, parsing failure, invalid orientation/command, initial out-of-bounds) |
| `3`  | Task end due to OOB / Collision in `fail-fast` mode                                                    |

# Non-Functional Requirements #
* Time Complexity:
* Space Complexity:
* Respect the Best practices
* High cohesion & Low coupling
* Maintainable, reusable and Scalability
* Testing is obligatory
* Java21

# Design #
1. Core Model
   * `Plateau{maxX, maxY}`: `isBounded(x, y)`
   * `Rover{x, y, direction, plateau}`: `execute(commands)`
   * `Direction{E, S, W, N}`: `rotateLeft()` `rotateRight()` `fromChar()` `(dx, dy)`
2. Edge Cases
   * When (maxX, maxY) of the plateau is (0 0): only one cell (0,0) exists, and any M will be out of bounds.
   * Rover group incomplete (only position lines present, no instruction lines): Report error and pinpoint to specific line.
   * The command line contains an invalid character (not L/R/M): Report error and the character's position is indicated.
3. Logger: JSON
    * Level:
      * | scenario                                                     | level     |                                                  |
        |--------------------------------------------------------------|-----------|--------------------------------------------------|
        | Start/End/Turn/Finished Move                                 | `INFO`    | `TURN / MOVE_FINISHED / START_ROVER / END_ROVER` |
        | OOB/Collision when `fail-fast-per-Rover` or `skip-step` Mode | `WARNING` | Record, current rover stop or skip this step     |
        | OOB/Collision when `fail-fast` Mode                          | `ERROR`   | End the process, with `ABORT_MISSION`            |
    * Event:
      * Lifecycle: `RUN_START`, `RUN_END`, `START_ROVER`, `END_ROVER`, `ABORT_MISSION`
      * Command: `TURN`, `MOVE_FINISHED`, `SKIPPED`(when `skip-step` Mode)
      * OOB/Collision: `FAULT_OOB`, `FAULT_OCCUPIED`
      * END of rover: `STOPPED_ROVER`(when `fail-fast-per-Rover` Mode)
    * Fields:
      * | Field         | type   |                                                                                                                 |
        |---------------|--------|-----------------------------------------------------------------------------------------------------------------|
        | ts            | string | Timestamp                                                                                                       |
        | level         | string | `INFO`/`WARNING`/`ERROR`                                                                                        |
        | mode          | string | `fail-fast`/`fail-fastper-rover`/`skip-step`                                                                    |
        | roverId       | int    | rover number                                                                                                    |
        | stepId        | int    | The 1-based step index within the instruction string (appears only for instruction events/OOB/Collision events) |
        | cmd           | string | Current command `L`/`R`/`M`                                                                                     |
        | x, y          | int    | Current position (before command)                                                                               |
        | nx, ny        | int    | Position after move (appears with `MOVE_*`                                                                      |
        | headingBefore | string | Current orientation (before command)                                                                            |
        | headingAfter  | string | Orientation after command                                                                                       |
        | fault         | object | `{"type": OOB/OCCUPIED, "reason":}`                                                                             |
        | plateau       | object | `{ "maxX":int, "maxY":int }` (with `RUN_START`)                                                                 |
        | lines         | object | `{ "positionLine": int, "commandsLine": int }` (with `START_ROVER`)                                             |

# Usage #
`java -jar target/rover-all.jar input.txt 1>positions.txt 2>logs.ndjson`