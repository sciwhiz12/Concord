This document contains information pertinent to contributors and maintainers.

## Building and publishing

Run the `build` task (or `assemble` to skip any checks), and check the `build/libs` folder. There are three JARs
created:

- The non-classified JAR is the full mod with all dependencies shaded in using the Gradle shadow plugin. This is what
  should be distributed to users and testers.
- The `-sources` JAR contains the (non-obfuscated) Java source files and assets of the mod. This exists for publishing
  to Maven repositories, for the benefit of IDE users.
- The `-lite` JAR is the mod without any of its dependencies. This is what developers can build against without
  encountering the problems of shadowed dependencies.

By default, the `projectLocal` publishing repository is defined to the `repo` folder under the project directory.
Running `publish` will generate the publications into that directory, for potential inclusion as a temporary file-based
Maven repository by other local projects.

It is recommended to distribute the artifacts directly from `build/libs` rather than from the project local repository,
because the Maven published artifacts do not hold the abbreviated commit hash in the filename (for non-tagged versions).

## Messages and translations

New translatable messages (those presented to the user in-game) should be defined in `Translations`. New Discord
messages (those sent to the linked Discord channel) should be defined in `Messages`.

When adding a new message or modifying an existing one substantially:

- Check the current version in the `TRANSLATIONS` entry in `FeatureVersion`. If the comment which matches the current
  version listed in the enum value indicates the correct future version, then use the current version. Otherwise, bump
  the current version's minor version, add a new comment with the new current version and the correct future version
  (following the same format), and use that new current version.
- Add the new message or modify the existing message as usual.
- Replace the last modified version parameter with the current version taken above.

For modifying existing messages, the above steps for the last modified version parameter need only be done for
substantial changes. The meaning of this is hard to define, but as a general view, any change which alters the semantics
of the existing message should have an updated last modified version.

For example, this includes removing or reordering existing arguments, and (most of the time) adding new arguments. If
in doubt, the maintainers will also mark any messages which needs updating of the last modified verison.

Specifying the correct last modified version allows the translations subsystem to correctly figure out when to eagerly
or lazily translate certain keys.

## Formatting

### License headers

After adding a new file, run the `updateLicenses` command to apply the license header to the new file. These headers
are automatically checked by the `build` task (through `check`), and are therefore checked by the CI as well.

### Code style

The code style of the project is simple, and usually follows the defaults of IntelliJ IDEA.

- 4 spaces of indentation (no tabs), 8 spaces for continuation indentation.
- Wildcard imports for more than 5 imports from a single package.
- Braces on the same line as the declaration.
- Use `@Nullable` in any place where `null` may be present, including local variables.
- Packages should have a `package-info.java` with the default non-nullability annotations.
- A general line limit of 90 characters (but this can be waived in some cases).
- Local variables should be `final` when possible and not `var` (for online readability).

## Pull request etiquette

- Check that the style of new code conforms to the project's code style.
- For new files, run the `updateLicenses` task to add the license header.
- Compile and build the project to ensure no compilation errors exist.
- Test changes where possible as much as possible.
- Document new features in the code through documentation comments and in the pull request description.
- Write anything noteworthy in the pull request description.

## Versioning

Versioning is done by the [simplversion](https://github.com/sciwhiz12/simplversion) plugin.

The current version calculated for the workspace is printed to console during each invocation of Gradle. Unless the
workspace is on a commit for a release version tag, the simple version (without classifiers and such) is the next
version to be released in the future.

The JAR manifest contains the simple version, the full version, the full commit hash, and the commit timestamp.

### Prefix stripping

Certain prefixes, such as the current branch name, the Minecraft version, and the conventional `v` prefix, are stripped
from the tag before being used as the version. For example, if building on a branch configured for MC version 1.18.1,
the tag `1.18.1-v1.2.0` will be parsed as `1.2.0`. Other examples which parse to the same output include `1.2.0`
(itself), `v1.2.0` (itself with the `v` prefix), and `1.18.1-1.2.0` (MC version, without `v` prefix).

### Version calculation

- For tagged commits, the version is exactly as defined by the tag.
- If the workspace has uncommitted changes or if it is on a non-tagged commit, the `-SNAPSHOT` classifier is appended
  after any other classifiers.
- The versioning system automatically increments the minor version by one since the last tagged version if on a
  non-tagged
  commit. For example, if the last tagged version is `1.3.0`, then the calculated version will be `1.4.0`.
- For tagged versions with qualifiers, such as `-alpha.3`, `-beta.1`, `-rc2`, the minor version will _not be
  incremented_.
  For example, if the last tagged version is `1.2.0-pre2`, then the calculated version is `1.2.0-pre2` and _not_
  `1.3.0-pre2`.
- For non-tagged commits, the built JARs in the `build/libs` folder will have the abbreviated commit hash appended to
  the
  filename and to the version stored in the JAR manifest.
