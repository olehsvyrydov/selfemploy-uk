# JavaFX MCP Server

An MCP (Model Context Protocol) stdio server for automating the SelfEmploy JavaFX
desktop app — or any desktop application — from an AI coding agent. It provides
screen-level automation: screenshots the agent can actually see, plus mouse and
keyboard input.

## Tools

| Tool | Description |
|---|---|
| `screenshot` | Capture the screen (X11) or an application window (Wayland) and return it as an image. Optional region (`x`/`y`/`width`/`height`, screen coordinates), `windowTitle` filter, and `savePath`. |
| `list_windows` | List visible application windows with id, title, position and size. |
| `click` | Click at screen coordinates (`button`: left/right/middle, `clicks`). |
| `move_mouse` | Move the pointer to screen coordinates. |
| `type_text` | Type text via synthetic key events. |
| `press_key` | Press a named key (Enter, Tab, Escape, arrows, F1–F12, …). |
| `scroll` | Scroll the mouse wheel, optionally at a position. |
| `get_screen_info` | Screen size, mouse position, session type. |
| `wait` | Sleep for N milliseconds (max 60 s). |

## Install

Requirements: Java 21+, Maven, and (on Linux) the `x11-utils` package (`xwd`, `xwininfo` —
preinstalled on Ubuntu).

**This repository (Claude Code):** already registered in the project's `.mcp.json`;
it builds itself on first launch. Nothing to do.

**Any other MCP client / another checkout:**

```bash
claude mcp add javafx-automation -- /path/to/selfemploy-uk/tools/javafx-mcp-server/run.sh
```

or in an `mcpServers` JSON config:

```json
"javafx-automation": { "command": "/path/to/selfemploy-uk/tools/javafx-mcp-server/run.sh" }
```

`run.sh` compiles the server on first use (or after source changes) and then serves
MCP over stdio.

## Wayland

`java.awt.Robot.createScreenCapture` does not work in Wayland sessions: the JDK's
portal/PipeWire path blocks on a permission dialog, and the plain X11 path sees only
a black XWayland root window. This server therefore captures **individual application
windows** via `xwd` on Wayland. JavaFX (and all X11/XWayland) windows capture fine;
native Wayland surfaces (e.g. GNOME Shell itself) do not appear.

Input events are injected through XTest, which reaches XWayland clients — so the
JavaFX app can be clicked and typed into, but native Wayland windows cannot.

Every screenshot response includes the window's screen position and the exact
formula for converting image pixels to screen coordinates for clicking.

## Test

```bash
./smoke-test.sh
```
