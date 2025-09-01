# ModFest Mod Templates!

Mod templates tailored towards use at [ModFest](https://modfest.net/) events. Uses local copying to not pollute your commit log (or repo header).

## To Use:

### Reserve your Modrinth slug

On [Modrinth](https://modrinth.com/):
- Click `+`->`🧊 New Project`
- Enter a mod name and short summary of your intended mod
- Leave the URL (aka "mod slug") as default (`name-of-your-mod` in kebab case)
- Click `+ Create Project`

If the mod name is taken, come up with a new name!

### Set up your mod repo

On this template page:
- Click `🔌 X Branches` and select your desired platform
- Click the green `< > Code` button and select `Download as Zip`

Anywhere on github:
- On the top bar, click `+ (Create new...)` -> `New Repository`
- Type your **mod slug** under "repository name"
  - This should match your modrinth URL
- Check "add license" and pick one (we recommend [EUPL-1.2](https://choosealicense.com/licenses/eupl-1.2) - click `MIT` and copy it in later)
- Click `Create repository`

On your new **mod repo page**:
- Click `About ⚙️` and enter a short mod summary
- Click `🔌 X Branches`, above the file view and click `View all branches`
- On `main`, click `...`->`✏️ Rename branch` and enter the branch name you downloaded
- Click the `< > Code` tab to return to the main repo page
- Click the green `< > Code` button left of the about section and clone the repo locally using `git` or `gh` as indicated
- Copy the contents of the previously downloaded zip (excluding LICENSE) into your cloned folder.
- Open the cloned folder in your preferred IDE, e.g. [IntelliJ IDEA CE](https://www.jetbrains.com/idea/download#community-edition)

### Correct the metadata

In your IDE:
- Commit the template (Ctrl+K in IntelliJ), adding all files, with the message `modfest template`
- Copy any preferred license content into `LICENSE` (e.g. [EUPL-1.2](https://choosealicense.com/licenses/eupl-1.2))
- Open `gradle.properties` and replace the following:
  - `group` to `io.github.{username}` (as below) or if you own a domain e.g. `hostname.tld`, you can use `tld.hostname`
  - `username` to your github username (as it appears in your repo URL)
  - `slug` to your mod slug (as it appears in github and modrinth URLs)
  - `modId` to your mod ID (in snake case, e.g. `name_of_your_mod` - not too long!)
  - `modDescription` to your short mod summary
  - `authors` to your handle/name, along with any other authors (comma-space-separated - `me, you, another`)
  - `contributors` to anyone who helped you (comma-space-separated - `me, you, another`), or just blank (`contributors=`)
  - `license` to your license identifier (e.g. `MIT` or `EUPL-1.2`)
- Expand `src/main/java/io/github/username/modid/` in the left pane
  - Double click `🇨 ModId` and edit `"mod_id"` and `[Mod ID]` to your ID and mod name respectively.
    - Optionally, uncomment the logger line and change it to have a unique message
  - Right click `🇨 ModId` and select `Rename...`, then enter your Mod ID in PascalCase and click `Refactor`
  - Directly above, right click `📁 modid` and select `Rename...`, then enter `{group}.{modid}` (ID in flatcase)
- Expand `src/main/resources/assets/mod_id` in the left pane
  - Open `mod_id.mixins.json` and edit `io.github.username.mixin` to `{group}.{modid}.mixin`
  - Right click `mod_id.mixins.json` and select `Rename...`, replacing `mod_id` with your mod ID (in snake_case)
  - Crack open `icon.png` in an image editor and scribble something vaguely resembling your mod
  - Below `assets`, right click `mod_id` and select `Rename...`, entering your mod ID (in snake_case)
- Run `./gradlew runClient` to validate the mod launches
- Commit and push these changes (Ctrl+K in IntelliJ) with the message `initial commit` - check "amend" if you prefer one initial commit.

### Develop your mod

Check out the [fabric modding wiki](https://docs.fabricmc.net/develop/getting-started/introduction-to-fabric-and-modding) for the basics on your version!

Note that, for this template:
- Changing the mod description via `README.md`, not modrinth!
- Metadata usually in `fabric.mod.json` has been migrated to `gradle.properties` for easy editing
- Dependency versions usually in `build.gradle` are in `libs.versions.toml` for programmatic use 
- Whenever you add a new feature, always use `./gradlew runClient` to test it, then push a commit describing your changes!

### Enable Modrinth publishing

On Modrinth's [Personal access tokens](https://modrinth.com/settings/pats) page:
- Click `+ Create a PAT`
- Enter `Github Actions` as a name
- Check `Create versions` and `Write projects`
- Set the expiry to 1 year from the current date
- Click `+ Create PAT`
- Click the `📋` button that appears with a long secret string

On your **mod repo page**:
- Click `⚙️ Settings`->`Security`->`Secrets and variables`->`Actions`
- Click `New Repository Secret`
- Enter `MODRINTH_TOKEN` as the name, and paste your secret string from modrinth
- Click `Add Secret`

### Make a release

On your **mod repo page**:
- Click `Releases` on the right pane
- Click `Draft a new release`
- Click `Choose a tag`, and enter the `modVersion` set in `gradle.properties`, then click `+ Create a new tag`
  - If you're using multiple branches to support multiple versions, you should use `modVersion+branchName` instead
- Enter a description of your changes, e.g. `Initial Release` if this is the first release
- Click `Generate release notes` to append useful links
- Click `Publish release` - *without* attaching any files!

After a few minutes:
- The mod jar will appear on the github release page
- A matching release will be created on the modrinth page.
 
You can review this process on the repo page under `▶️ Actions`.

## Notes

### .gitignore

We included a thorough `.gitignore` for you, which blocks you from accidentally publishing files you didn't intend to.

If one of the rules gets in the way, you can use `!` to add rules of what to allow.
One such example would be `!src/main/resources/template.log` to allow bundling template.log in your mod.
