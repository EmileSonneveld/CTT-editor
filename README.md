CTT-editor
==========

Concurrent Task Tree editor

A straightforward way to write CTTs in plain text, with a fast visualiser.


TODO:
-----
- Be able to refer to tasks defined in another file.
- Generate enabled task sets
	- Check more cases with Disabling task
	- Allow to collapse []>> operators
	- Slide 11, how does second way to interpret an [optional-task] work?
- Generate HTML pages from CTT
- Check CTT on problems:
	- Syntax in text file
	- exercise 1: A looping task without deactivation
	- exercise 2: Or operations [] between optional tasks
	- exercise 3: Ambiguity in order of operations
	- exercise 4: Select task, before something is shown
	- exercise 6: UI specific words, like 'click'
	- Slide 20: With option task, the first choises should not be application tasks


Some things I learned:
----------------------

- Scala is a very smooth programming language. But has to many features.
- Maven dependencies just seem to work.
- Java-FX has no descend way to render SVG
	- When using a webview, assets with a relative path are not loaded.
- Using el.innerHTML is very fast. I use it to load the SVG in the html page, to be less dependent on platform specific code.
- Sync requests don't seem that bad when the site is always loading on localhost. A request takes ~ 10ms.
- The backend (CrudServer) is very simple backend that gives the user all file permissions in the folder where it operates.
For applications running locally, this isn't a security problem. For online applications, this should only be used in small teams with people you trust.


Building
--------
CrudServer and ctt-editor-fastopt.js need to be build separatly.
Copy them to the correct location and commit to save a new version.
