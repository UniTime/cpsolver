# CPSolver

Local-search based solver of Constraint Satisfaction and Optimization Problems
<http://www.cpsolver.org>

The constraint solver library contains a local search based framework that allows modeling of
a problem using constraint programming primitives (variables, values, constraints).

The search is based on an iterative forward search algorithm. This algorithm is similar to local
search methods; however, in contrast to classical local search techniques, it operates over feasible,
though not necessarily complete, solutions. In these solutions, some variables may be left unassigned.
All hard constraints on assigned variables must be satisfie,d however. Such solutions are easier to
visualize and more meaningful to human users than complete but infeasible solutions. Because of the
iterative character of the algorithm, the solver can also easily start, stop, or continue from any
feasible solution, either complete or incomplete.

The framework also supports dynamic aspects of the minimal perturbation problem, allowing the number
of changes to the solution (perturbations) to be kept as small as possible.

The constraint solver was among the finalists for all three tracks of the [International Timetabling
Competition 2007][itc2007] (ITC2007) and it won two of them, see [ITC 2007][cpsolver-itc2007] for more details.

The solver was also used for validation of the [International Timetabling
Competition 2019][itc2019] (ITC2019), which is based on the course timetabling problem of [UniTime].
The ITC2019 solver extension is available in the [itc2019-solver] repository.

### Components

The following modules are included in the library:
- Local-search based solver of Constraint Satisfaction and Optimization Problems
- Course Timetabling Extension
- Student Sectioning Extension
- Examination Timetabling Extension
- Instructor Scheduling Extension

### Links
- [API Documentation][api]
- [Examples][examples]
- [ITC 2007][cpsolver-itc2007]
- [ITC 2019][itc2019-solver]
- [Nightly Builds][builds]
- [Downloads][downloads]

[UniTime]: https://www.unitime.org
[itc2007]: http://www.cs.qub.ac.uk/itc2007
[itc2019]: https://www.itc2019.org
[cpsolver-itc2007]: https://www.cpsolver.org/itc2007
[api]: https://www.unitime.org/api/cpsolver-1.4
[examples]: https://www.unitime.org/cpsolver_examples.php
[builds]: http://builds.unitime.org/#CPSolver14
[downloads]: https://sourceforge.net/projects/cpsolver/files/cpsolver
[itc2019-solver]: https://github.com/tomas-muller/itc2019-solver
