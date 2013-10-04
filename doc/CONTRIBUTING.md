## Contributing to Sketchy
In the spirit of [free software][free-sw], **everyone** is encouraged to help
improve this project. Here are some ways that  *you* can contribute:

* Use alpha, beta, and pre-released software versions.
* Report bugs.
* Suggest new features.
* Write or edit documentation.
* Write specifications.
* Write code; **no patch is too small**: fix typos, add comments, clean up
  inconsistent whitespace, check the [TODO](TODO.md).
* Refactor code.
* Fix [issues][].
* Review patches.

## Submitting an issue
We use the [GitHub issue tracker][issues] to track bugs and features. Before
you submit a bug report or feature request, check to make sure that it has not
already been submitted. When you submit a bug report, include
a [Gist][] that includes a stack trace and any details that might be necessary
to reproduce the bug. Ideally, a bug report includes a pull request with
failure specifications.

## Submitting a pull request
1. [Fork the repository][fork].
2. [Create a topic branch][branch].
3. Add tests for your un-implemented feature or bug fix.
4. Run `echo test | make sbt`. If your tests pass, return to the previous step.
5. Implement your feature or bug fix.
6. Run `echo test | make sbt`. If your tests fail, return to the previous step.
7. If your tests do not completely cover your changes, return to step 3.
8. Add documentation for your feature or bug fix.
9. Commit and push your changes.
10. [Submit a pull request][pr].

<!-- Alphabetize list: -->
[branch]: http://learn.github.com/p/branching.html
[fork]: http://help.github.com/fork-a-repo
[free-sw]: http://www.fsf.org/licensing/essays/free-sw.html
[gist]: https://gist.github.com
[issues]: https://github.com/soundcloud/sketchy-core/issues
[pr]: http://help.github.com/send-pull-requests
