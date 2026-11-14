# Compose client structure

The shared client is organized by responsibility:

- app — application root and shared Compose entrypoint
- navigation — screen routing
- core/model — client models and API DTOs
- core/network — Ktor HTTP client and platform engines
- core/session — token and current user state
- core/ui — shared theme, components, loading and error states
- feature/auth — login screen and authentication state
- feature/equipment — list, filters, card, create and edit forms
- feature/history — equipment movement and status history
- feature/references — categories, models, locations and employees
- feature/reports — summary and CSV actions

Platform modules contain only platform startup code:

- desktopApp — window creation and desktop-specific configuration
- androidApp — Activity, Android permissions and Android-specific configuration

Business rules and HTTP calls belong in shared code. Screens should talk to ViewModels or feature state holders, not call Ktor directly.
