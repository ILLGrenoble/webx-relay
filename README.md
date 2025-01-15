# WebX Relay

## Description

The WebX Relay provides a Java library to connect a WebX Host (typically serving a WebX Router for multiuser WebX Sessions or a WebX Engine for standalone testing) to a client (typically connected through a websocket).

The client-side design of the WebX Relay is generic allowing any type of connection being made to the WebX Host. Specifically, the WebX Router has been designed to be embedded in a Java Web Application to act as a bridge between a browser and a Remote Desktop.  

The WebX Router is designed to perform trafficking between multiple WebX hosts and clients.

Connections to hosts are made using ZeroMQ TCP sockets (implemented here with the JeroMQ library).

## WebX Overview

WebX is a Remote Desktop technology allowing an X11 desktop to be rendered in a user's browser. It's aim is to allow a secure connection between a user's browser and a remote linux machine such that the user's desktop can be displayed and interacted with, ideally producing the effect that the remote machine is behaving as a local PC.

WebX's principal differentiation to other Remote Desktop technologies is that it manages individual windows within the display rather than treating the desktop as a single image. A couple of advantages with a window-based protocol is that window movement events are efficiently passed to clients (rather than graphically updating regions of the desktop) and similarly it avoids <em>tearing</em> render effects during the movement. WebX aims to optimise the flow of data from the window region capture, the transfer of data and client rendering.

> The full source code is openly available and the technology stack can be (relatively) easily demoed but it should be currently considered a work in progress.

The WebX remote desktop stack is composed of a number of different projects:
- [WebX Engine](https://github.com/ILLGrenoble/webx-engine) The WebX Engine is the core of WebX providing a server that connects to an X11 display obtaining window parameters and images. It listens to X11 events and forwards event data to connected clients. Remote clients similarly interact with the desktop and the actions they send to the WebX Engine are forwarded to X11.
- [WebX Router](https://github.com/ILLGrenoble/webx-router) The WebX Router manages multiple WebX sessions on single host, routing traffic between running WebX Engines and the WebX Relay.
- [WebX Session Manager](https://github.com/ILLGrenoble/webx-session-manager) The WebX Session manager is used by the WebX Router to authenticate and initiate new WebX sessions. X11 displays and desktop managers are spawned when new clients successfully authenticate.
- [WebX Relay](https://github.com/ILLGrenoble/webx-relay) The WebX Relay provides a Java library that can be integrated into the backend of a web application, providing bridge functionality between WebX host machines and client browsers. TCP sockets (using the ZMQ protocol) connect the relay to host machines and websockets connect the client browsers to the relay. The relay transports data between a specific client and corresponding WebX Router/Engine.
- [WebX Client](https://github.com/ILLGrenoble/webx-client) The WebX Client is a javascript package (available via NPM) that provides rendering capabilities for the remote desktop and transfers user input events to the WebX Engine via the relay.

To showcase the WebX technology, a demo is available. The demo also allows for simplified testing of the WebX remote desktop stack. The projects used for the demo are:
- [WebX Demo Server](https://github.com/ILLGrenoble/webx-demo-server) The WebX Demo Server is a simple Java backend integrating the WebX Relay. It can manage a multiuser environment using the full WebX stack, or simply connect to a single user, <em>standalone</em> WebX Engine.
- [WebX Demo Client](https://github.com/ILLGrenoble/webx-demo-client) The WebX Demo Client provides a simple web frontend packaged with the WebX Client library. The demo includes some useful debug features that help with the development and testing of WebX.
- [WebX Demo Deploy](https://github.com/ILLGrenoble/webx-demo-deploy) The WebX Demo Deploy project allows for a one line deployment of the demo application. The server and client are run in a docker compose stack along with an Nginx reverse proxy. This provides a very simple way of connecting to a running WebX Engine for testing purposes.

The following projects assist in the development of WebX:
- [WebX Dev Environment](https://github.com/ILLGrenoble/webx-dev-env) This provides a number of Docker environments that contain the necessary libraries and applications to build and run a WebX Engine in a container. Xorg and Xfce4 are both launched when the container is started. Mounting the WebX Engine source inside the container allows it to be built there too.
- [WebX Dev Workspace](https://github.com/ILLGrenoble/webx-dev-workspace) The WebX Dev Workspace regroups the WebX Engine, WebX Router and WebX Session Manager as git submodules and provides a devcontainer environment with the necessary build and runtime tools to develop and debug all three projects in a single docker environment. Combined with the WebX Demo Deploy project it provides an ideal way of developing and testing the full WebX remote desktop stack.

## Development

### Building the jar

To build the WebX Relay library run the following command:

```
mvn package
```

The built `jar` is in the `target` folder.

### Development with the WebX Demo and WebX Dev Workspace

Development of the functionality of the relay is most easily made with the [WebX Demo Server](https://github.com/ILLGrenoble/webx-demo-server) which provides a fully functional backend server. The [WebX Demo Client](https://github.com/ILLGrenoble/webx-demo-client) also provides an easy way to connect to the server.

To have a fully functional WebX stack, the easiest way is to run the [WebX Dev Workspace](https://github.com/ILLGrenoble/webx-dev-workspace) either with a standalone WebX Engine or a multiuser WebX Router. Please see the README in this project for more details.

### Setting up IntelliJ to build the WebX Demo Server with the WebX Relay

Open the WebX Demo Server project and the use the <b>File > Project Structure</b> menu to import the WebX Relay module.

Under <b>Modules</b> click the `+` button and choose <b>Import Module</b>. Navigate to the WebX Relay project and import it. Choose <b>Import module from existing model</b> and select <b>Maven</b>.

IntelliJ will now treat the dependency project as part of the workspace and changes made to the WebX Relay can be tested immediately in the WebX Demo Server.

## Maven project integration 

The WebX Relay is available from the [Maven Repository](https://mvnrepository.com/artifact/eu.ill/webx-relay) and the jar is easily added to a project.

To integrate WebX Relay into an existing Maven project add the following dependency:

```
<dependencies>
    ...
    <dependency>
        <groupId>eu.ill</groupId>
        <artifactId>webx-relay</artifactId>
        <version>${webx-relay.version}</version>
    </dependency>
</dependencies>

