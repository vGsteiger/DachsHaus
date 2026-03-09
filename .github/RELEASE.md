# Release Process

## Automated Releases

DachsHaus uses automated releases via GitHub Actions. When a PR is merged to the `main` branch, a new release is automatically created based on the commit messages.

## Version Numbering

We follow [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH):

- **MAJOR** version: Breaking changes (commit messages with `BREAKING CHANGE:` or `breaking:`)
- **MINOR** version: New features (commit messages starting with `feat:` or `feature:`)
- **PATCH** version: Bug fixes and other changes (all other commits)

## Commit Message Convention

To ensure proper version bumping and release notes generation, follow this commit message format:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Types:
- `feat:` - New feature (triggers minor version bump)
- `fix:` - Bug fix (triggers patch version bump)
- `docs:` - Documentation changes
- `chore:` - Maintenance tasks
- `ci:` - CI/CD changes
- `build:` - Build system changes

### Examples:

```bash
# Feature (minor version bump)
feat(catalog): add product search functionality

# Bug fix (patch version bump)
fix(cart): resolve issue with cart item removal

# Breaking change (major version bump)
feat(auth)!: change authentication flow

BREAKING CHANGE: Authentication now requires refresh tokens
```

## Release Notes

Release notes are automatically generated from commit messages and grouped by type:
- ✨ Features
- 🐛 Bug Fixes
- 📚 Documentation
- 🔧 Maintenance
- 🔄 Other Changes

## Manual Releases

To trigger a release manually:

1. Go to Actions → Release workflow
2. Click "Run workflow"
3. Select the `main` branch
4. Click "Run workflow"

## Skipping Releases

To skip automatic release creation (e.g., for documentation-only changes), include `[skip ci]` in your commit message:

```bash
docs: update README [skip ci]
```

## Initial Release

The first release will be `v0.1.0` if the first commit after setup is a feature, or `v0.0.1` for other types of commits.
