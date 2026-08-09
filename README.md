# Whatsapp-ME

A Java ME (J2ME) mobile messaging client built with NetBeans. The project targets MIDP/CLDC and connects to a local backend API for chat syncing, QR code login, and message exchange.

## Project structure

- `src/` - Java source files for the MIDlet, UI screens, services, storage, and utilities.
- `build.xml` - Ant build file that imports NetBeans-generated targets from `nbproject/build-impl.xml`.
- `nbproject/` - NetBeans project configuration for the J2ME project.

## Features

- MIDlet entrypoint: `com.wpjava.WPJavaMidlet`
- Local API backend URL configured in `src/com/wpjava/core/Config.java`
- Supports chat listing, message history, QR login, and sync service

## Requirements

- NetBeans with Java ME (Mobility) support
- A Java ME compatible emulator or device
- Local backend service running at `http://localhost:3000`

## Build and run

1. Open the project in NetBeans.
2. Build the project or run the `jar` target.
3. Deploy and run on a Java ME emulator or device.

### Command line

From the project root, use Ant:

```bash
ant jar
```

To clean build artifacts:

```bash
ant clean
```

## Configuration

The backend endpoint is defined in `src/com/wpjava/core/Config.java`:

```java
public static final String API_URL = "http://localhost:3000 ";
```

Update this value if you want to point the app to a different backend.

## Notes

- The app is intended for a legacy Java ME environment and uses MIDP UI building blocks.
- The backend API must expose endpoints such as `/status`, `/qr`, `/messaging-history`, `/chats`, and `/send`.

## License

No license is included in this repository. Add one if you want to open-source the project.