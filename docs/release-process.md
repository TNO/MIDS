# Releasing MIDS

To release a new release candidate or final release of MIDS, follow these steps:

* In GitHub, on the main page of MIDS, click in the right bar on *Create a new release*.
* Click *Choose a tag*, enter the new release name, e.g., `v0.9`, `v1.0-RC1`, or `v1.0.1-RC2`, and click *Create new tag: <tagname> on publish*, where *<tagname>* is the name of the tag.
* For *Release title* type the same name as the tag name.
* Type a description of the release, based on the release notes for the release in the MIDS documentation.
See earlier releases for examples.
* For release candidates, check the *Set as a pre-release* option.
* Click *Save draft* or *Publish release*.
If saved as a draft, it can be edited again, until finally *Publish release* is clicked.
* Once published, the release and tag are created.
The GitHub action will then build the release, and attach the built website and products to the release.
* Download the website archive from the release.
* Check out the `website` branch, and make sure it is up to date.
* Remove the old website contents from the branch:
  * Remove all files in the root of the branch, except for the `.git*` files.
  * Remove the `userguide` and `images` folders.
* Extract the new website archive into the branch.
* Add the changes by executing `git add -A .` in a shell.
* Commit the changes by executing `git commit -m "Replaced website by version <release_name>."` in a shell, replacing `<release_name>` by the name of the release.
* Push the changes by executing `git push` in a shell.
* It may take a few minutes before the website is deployed by GitHub and it becomes visible in a browser.
