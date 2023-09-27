# Setting up a MIDS development environment

## Prerequisites

Before setting up the development environment, GitHub should correctly be set up regarding SSH keys (in order to check out the repository using SSH).
Instructions for generating new SSH keys and adding them to your GitHub account can be found [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh).
       
## Setup

Use the following steps to set up an Eclipse development environment for MIDS development:

1. Download the Eclipse Installer, from https://eclipse.org/downloads.
2. Run the Eclipse Installer.
3. Switch to Advanced mode, using the hamburger menu.
4. Select *Eclipse Modeling Tools*, *2022-03* and *JRE 11.x - https://download.eclipse.org/justj/jres/11/updates/release/latest*.
5. Click *Next*.
6. Use the green plus button to add `https://raw.githubusercontent.com/TNO/MIDS/main/releng/mids.setup`.
   Choose *Catalog: Eclipse Projects* in the dropdown box.
7. Select *MIDS* and press *Next*.
8. Enable *Show all variables* and configure *Root install folder*, *Installation folder name*, *GitHub account full name* and *GitHub account email address*.
9. Click *Next* and then click *Finish*.
10. When the installer asks trusting licenses and content, accept all licenses and trust all content from all authorities.
    Multiple such popups may appear.
11. Once the installer is done, and a new development environment is launched, click *Finish* in the installer to close it.
12. Wait for the remaining installation tasks to be completed, including the checkout of the Git repository, importing the projects, setting up the target platform, and building all projects.
