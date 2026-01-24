package xyz.yourboykyle.secretroutes.routes.data;

import java.util.List;

public record RoomSecrets(String roomName, List<Secret> secrets) {}
