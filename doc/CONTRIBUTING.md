## Contributing
In the spirit of [free software][free-sw], **everyone** is encouraged to help
improve this project.

[free-sw]: http://www.fsf.org/licensing/essays/free-sw.html

Here are some ways *you* can contribute:

* Use alpha, beta, and pre-released software versions
* Report bugs
* Suggest new features
* Write or edit documentation
* Write specifications
* Write code; **no patch is too small**: fix typos, add comments, clean up
  inconsistent whitespace, check [TODO](TODO.md) for our wish list
* Refactor code
* Fix [issues][]
* Review patches

[issues]: https://github.com/soundcloud/sketchy-core/issues

## Submitting an issue
We use the [GitHub issue tracker][issues] to track bugs and features. Before
submitting a bug report or feature request, check to make sure it has not
already been submitted. When submitting a bug report, include
a [Gist][] that includes a stack trace and any details that might be necessary
to reproduce the bug. Ideally, a bug report includes a pull request with
failing specifications.

[gist]: https://gist.github.com/

## Submitting a pull request
1. [Fork the repository][fork].
2. [Create a topic branch][branch].
3. Add specifications for your un-implemented feature or bug fix.
4. Run `echo test | make sbt`. If your specifications pass, return to the previous step.
5. Implement your feature or bug fix.
6. Run `echo test | make sbt`. If your specs fail, return to the previous step.
7. If your tests do not completely cover your changes, return to step 3.
8. Add documentation for your feature or bug fix.
9. Commit and push your changes.
10. [Submit a pull request][pr].

[fork]: http://help.github.com/fork-a-repo/
[branch]: http://learn.github.com/p/branching.html
[pr]: http://help.github.com/send-pull-requests/
