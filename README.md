# CPSolver

Local-search based solver of Constraint Satisfaction and Optimization Problems
<http://www.cpsolver.org>

The constraint solver library contains a local search based framework that allows modeling of
a problem using constraint programming primitives (variables, values, constraints).

The search is based on an iterative forward search algorithm. This algorithm is similar to local
search methods; however, in contrast to classical local search techniques, it operates over feasible,
though not necessarily complete, solutions. In these solutions some variables may be left unassigned.
All hard constraints on assigned variables must be satisfied however. Such solutions are easier to
visualize and more meaningful to human users than complete but infeasible solutions. Because of the
iterative character of the algorithm, the solver can also easily start, stop, or continue from any
feasible solution, either complete or incomplete.

The framework also supports dynamic aspects of the minimal perturbation problem, allowing the number
of changes to the solution (perturbations) to be kept as small as possible.

The constraint solver was among finalists for all three tracks of the [International Timetabling
Competition 2007][itc2007] (ITC2007) and it won two of them, see [ITC 2007][cpsolver-itc2007] for more details.

### Components

The following modules are included in the library:
- Local-search based solver of Constraint Satisfaction and Optimization Problems
- Course Timetabling Extension
- Student Sectioning Extension
- Examination Timetabling Extension

### Links
- [API Documentation][api]
- [Examples][examples]
- [ITC 2007][cpsolver-itc2007]
- [Nightly Builds][builds]
- [Downloads][downloads]

[itc2007]: http://www.cs.qub.ac.uk/itc2007
[cpsolver-itc2007]: http://www.cpsolver.org/itc2007
[api]: http://www.unitime.org/api/cpsolver-1.3
[examples]: http://www.unitime.org/cpsolver_examples.php
[builds]: http://builds.unitime.org/#CPSolver13
[downloads]: https://sourceforge.net/projects/cpsolver/files/cpsolver
