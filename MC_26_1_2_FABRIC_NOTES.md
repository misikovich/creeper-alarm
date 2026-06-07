# Fabric Mod Setup for Minecraft 26.1.2

Mojang removed obfuscation in 26.1.2. No Yarn or official Mojang mappings exist — use a local `mappings.tiny` with `noIntermediateMappings()`. Class/method names in code use Mojang's official package structure (different from Yarn).

---

## File structure

```
mod-root/
├── mappings.tiny               ← required, see below
├── settings.gradle
├── build.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── src/main/resources/
    ├── fabric.mod.json
    └── modid.mixins.json
```

---

## mappings.tiny

Create this file at the repo root with exactly this content (one line):

```
tiny	2	0	official	named
```

This is an empty mapping declaration. Since the game ships with readable names, no actual mappings are needed — the file just satisfies Loom's requirement for a tiny v2 format with `official` and `named` namespaces.

---

## settings.gradle

```groovy
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

The Fabric maven entry is required to resolve `fabric-loom 1.16-SNAPSHOT`.

---

## gradle.properties

```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false
# Point to JDK 25 if not your system default:
# org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home

minecraft_version=26.1.2
loader_version=0.19.3
mod_version=1.0.0
maven_group=your.group
archives_base_name=your-mod-id
fabric_api_version=0.151.0+26.1.2
```

---

## build.gradle

```groovy
plugins {
    id 'fabric-loom' version '1.16-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

loom {
    noIntermediateMappings()
}

repositories {
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.layered {
        mappings(rootProject.file('mappings.tiny'))
    }
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": inputs.properties.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.encoding = "UTF-8"
    it.options.release = 25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    // Do NOT call withSourcesJar() — remapSourcesJar breaks with noIntermediateMappings
}

jar {
    inputs.property "archivesName", project.base.archivesName
    from("LICENSE") {
        rename { "${it}_${inputs.properties.archivesName}" }
    }
}

publishing {
    publications {
        create("mavenJava", MavenPublication) {
            artifactId = project.archives_base_name
            from components.java
        }
    }
}
```

---

## gradle/wrapper/gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Use **Gradle 9.4.0**. 9.5.0 is incompatible with `fabric-loom 1.16-SNAPSHOT`.

---

## src/main/resources/fabric.mod.json

```json
{
    "schemaVersion": 1,
    "id": "your-mod-id",
    "version": "${version}",
    "name": "Your Mod Name",
    "description": "Description",
    "authors": ["you"],
    "license": "MIT",
    "environment": "*",
    "entrypoints": {
        "main": ["your.group.YourModServer"],
        "client": ["your.group.YourModClient"]
    },
    "mixins": [
        "your-mod-id.mixins.json"
    ],
    "depends": {
        "fabricloader": ">=0.19.3",
        "minecraft": "~26.1.2",
        "java": ">=25",
        "fabric-api": "*"
    }
}
```

- `"environment": "*"` for both client+server, `"client"` for client-only
- Remove `"main"` entrypoint if client-only mod
- `${version}` is expanded by `processResources` from `mod_version` in `gradle.properties`

---

## src/main/resources/your-mod-id.mixins.json

```json
{
    "required": true,
    "package": "your.group.mixin",
    "compatibilityLevel": "JAVA_25",
    "mixins": [],
    "client": [],
    "injectors": {
        "defaultRequire": 1
    }
}
```

List server-side mixins in `"mixins"`, client-side in `"client"`.

---

## Yarn → Mojang name mapping

### Classes

| Old (Yarn)                                        | New (Mojang)                                                        |
|---------------------------------------------------|---------------------------------------------------------------------|
| `net.minecraft.util.Identifier`                   | `net.minecraft.resources.Identifier`                               |
| `net.minecraft.entity.mob.CreeperEntity`          | `net.minecraft.world.entity.monster.Creeper`                       |
| `net.minecraft.server.network.ServerPlayerEntity` | `net.minecraft.server.level.ServerPlayer`                          |
| `net.minecraft.server.world.ServerWorld`          | `net.minecraft.server.level.ServerLevel`                           |
| `net.minecraft.client.MinecraftClient`            | `net.minecraft.client.Minecraft`                                   |
| `net.minecraft.client.gui.DrawContext`            | `net.minecraft.client.gui.GuiGraphicsExtractor`                    |
| `net.minecraft.client.render.RenderTickCounter`   | `net.minecraft.client.DeltaTracker`                                |
| `net.minecraft.client.gl.RenderPipelines`         | `net.minecraft.client.renderer.RenderPipelines`                    |
| `net.minecraft.sound.SoundEvent`                  | `net.minecraft.sounds.SoundEvent`                                  |
| `net.minecraft.network.RegistryByteBuf`           | `net.minecraft.network.RegistryFriendlyByteBuf`                    |
| `net.minecraft.network.packet.CustomPayload`      | `net.minecraft.network.protocol.common.custom.CustomPacketPayload` |
| `net.minecraft.network.codec.PacketCodec`         | `net.minecraft.network.codec.StreamCodec`                          |
| `net.minecraft.network.codec.PacketCodecs`        | `net.minecraft.network.codec.ByteBufCodecs`                        |

### Methods

| Old                            | New                                         |
|--------------------------------|---------------------------------------------|
| `Identifier.of(ns, path)`      | `Identifier.fromNamespaceAndPath(ns, path)` |
| `SoundEvent.of(id)`            | `SoundEvent.createVariableRangeEvent(id)`   |
| `entity.getUuid()`             | `entity.getUUID()`                          |
| `aabb.expand(n)`               | `aabb.inflate(n)`                           |
| `window.getScaledWidth()`      | `window.getGuiScaledWidth()`                |
| `window.getScaledHeight()`     | `window.getGuiScaledHeight()`               |
| `drawContext.drawTexture(...)` | `guiGraphicsExtractor.blit(...)`            |
| `PacketCodecs.BOOLEAN`         | `ByteBufCodecs.BOOL`                        |

---

## Fabric API changes

### Custom networking payload

```java
public record MyPayload(boolean value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MyPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("modid", "channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MyPayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, MyPayload::value, MyPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}

// Server init — register S2C payload
PayloadTypeRegistry.clientboundPlay().register(MyPayload.TYPE, MyPayload.CODEC);

// Server — send to player
ServerPlayNetworking.send(player, new MyPayload(true));

// Client — receive (pass TYPE, not an ID)
ClientPlayNetworking.registerGlobalReceiver(MyPayload.TYPE, (payload, context) -> {
    boolean val = payload.value();
    Minecraft client = context.client();
});
```

`playS2C()` → `clientboundPlay()`, `playC2S()` → `serverboundPlay()`

### HUD rendering (HudRenderCallback removed)

```java
public class MyMod implements ClientModInitializer, HudElement {

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("modid", "my_overlay"), this
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int color = (alpha << 24) | 0xFFFFFF;
        ctx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, 0, 0, 0.0f, 0.0f, w, h, w, h, color);
    }
}
```

Imports: `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement` and `HudElementRegistry`

### Tick events

| Old                                | New                                  |
|------------------------------------|--------------------------------------|
| `ServerTickEvents.END_WORLD_TICK`  | `ServerTickEvents.END_LEVEL_TICK`    |
| `ClientTickEvents.END_CLIENT_TICK` | unchanged                            |

### Entity queries

```java
// Old: world.getEntitiesByClass(CreeperEntity.class, aabb, pred)
world.getEntities(EntityTypeTest.forClass(Creeper.class), aabb, pred)

// Old: world.getPlayers()  — no-arg overload gone
world.getPlayers(p -> true)
```
