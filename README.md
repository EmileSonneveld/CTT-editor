CTT-editor
==========

Concurrent task tree editor
A straightforward way to write CTTs in plain text, with a fast visualiser.


TODO:
-----
Be able to refer to tasks defined in an other file.
Generate enabled task sets
Generate HTML pages from CTT
Check CTT on problems:
- Syntax in text file
- exercise 1: A looping task without deactivation
- exercise 2: Or operations [] between optional tasks
- exercise 3: Ambiguety in order of operations
- exercise 4: Select task, before shomething is shown
- exercise 6: UI specific words, like 'click'


Some things I learned:
----------------------

- Scala is a very smooth programming language. But has to many features.

- Maven dependencies just seem to work.

- Java-FX has no descend way to render SVG
	When using a webview, assets with a relative path are not loaded.

- Using el.innerHTML is very fast. I use it to load the SVG in the html page, to be less dependent on platform specific code.

- Sync requests don't seemt that bad when the site is always loading on localhost. A request takes ~10ms~.

- The backend (CrudServer) is very simple backend that gives the user all file permissions in the folder where it operates.
For applications running locally, this isn't a security problem. For online applications, this should only be used in small teams with people you trust.
