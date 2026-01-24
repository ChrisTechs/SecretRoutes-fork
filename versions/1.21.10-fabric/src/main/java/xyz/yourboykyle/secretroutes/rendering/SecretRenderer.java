package xyz.yourboykyle.secretroutes.rendering;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.utils.multistorage.Triple;

import java.awt.*;
import java.util.List;

public class SecretRenderer {

    public static void renderWorld() {
        SecretRoutesManager manager = SecretRoutesManager.get();
        if (!manager.hasRoute()) return;

        SRMConfig config = SRMConfig.get();

        // Route Rendering
        if (config.wholeRoute) {
            for (int i = 0; i < manager.getTotalSteps(); i++) {
                renderStep(manager.getStep(i), config, i + 1, manager.getTotalSteps());
            }
        } else {
            RouteStep step = manager.getCurrentStep();
            if (step != null) {
                renderStep(step, config, manager.getCurrentStepIndex() + 1, manager.getTotalSteps());
            }
        }

        // All Secrets Overlay
        if (config.allSecrets) {
            for (Secret secret : manager.getAllSecrets()) {
                if (!manager.isSecretFound(secret.pos()) || config.renderComplete) {
                    renderSecret(secret, config, false); // False = not part of route sequence logic
                }
            }
        }

        if (manager.getCurrentLeverPos() != null) {
            BlockPos pos = toWorldPos(manager.getCurrentLeverPos());
            Vec3d vec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            SecretRenderBackend.addBox(vec, config.secretsInteract, config.secretsInteractFullBlock);

            if (config.interactsTextToggle) {
                SecretRenderBackend.addText(
                        config.interactsWaypointColor.formatting + "Locked Chest Lever",
                        vec.add(0.5, 0.5, 0.5),
                        0xFFFFFFFF,
                        config.interactsTextSize
                );
            }
        }
    }

    private static void renderStep(RouteStep step, SRMConfig config, int stepNumber, int totalSteps) {
        if (config.renderEtherwarps) renderComponents(step.etherwarps(), config.etherWarp, config.etherwarpFullBlock, "etherwarp", config.etherwarpsTextToggle, config.etherwarpsTextSize, config.etherwarpsWaypointColor, config.etherwarpsEnumToggle);
        if (config.renderMines) renderComponents(step.mines(), config.mine, config.mineFullBlock, "mine", config.minesTextToggle, config.minesTextSize, config.minesWaypointColor, config.minesEnumToggle);
        if (config.renderInteracts) renderComponents(step.interacts(), config.interacts, config.interactsFullBlock, "interact", config.interactsTextToggle, config.interactsTextSize, config.interactsWaypointColor, config.interactsEnumToggle);
        if (config.renderSuperboom) renderComponents(step.superbooms(), config.superbooms, config.superboomsFullBlock, "superboom", config.superboomsTextToggle, config.superboomsTextSize, config.superboomsWaypointColor, config.superboomsEnumToggle);

        if (config.renderEnderpearls && step.enderpearls() != null) {
            renderEnderPearls(step, config);
        }

        // Route Lines
        if (!step.lines().isEmpty()) {
            List<BlockPos> locations = step.lines();

            // Start Text
            if (stepNumber == 1 && config.startTextToggle) {
                BlockPos pos = toWorldPos(locations.getFirst());
                SecretRenderBackend.addText(
                        config.startWaypointColor.formatting + "Start",
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5),
                        0xFFFFFFFF,
                        config.startTextSize
                );
            }


            // Exit Text
            if (stepNumber == totalSteps && config.exitTextToggle) {
                BlockPos pos = toWorldPos(locations.getLast());
                SecretRenderBackend.addText(
                        config.exitWaypointColor.formatting + "Exit",
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5),
                        0xFFFFFFFF,
                        config.exitTextSize
                );
            }

            if (config.lineType == SRMConfig.LineType.LINES) {
                for (int i = 0; i < locations.size() - 1; i++) {
                    Vec3d start = toWorldVec(Vec3d.of(locations.get(i))).add(0.5, 0.5, 0.5);
                    Vec3d end = toWorldVec(Vec3d.of(locations.get(i+1))).add(0.5, 0.5, 0.5);
                    SecretRenderBackend.addLine(start, end, config.lineColor, config.width);
                }
            } else if (config.lineType == SRMConfig.LineType.PARTICLES) {
                ParticleEffect effect = getParticle(config.particles);
                double density = Math.max(0.1, config.particleDensity);

                for (int i = 0; i < locations.size() - 1; i++) {
                    Vec3d start = toWorldVec(Vec3d.of(locations.get(i))).add(0.5, 0.5, 0.5);
                    Vec3d end = toWorldVec(Vec3d.of(locations.get(i+1))).add(0.5, 0.5, 0.5);

                    double dist = start.distanceTo(end);
                    int count = (int) (dist * density);
                    Vec3d dir = end.subtract(start).normalize();

                    for (int j = 0; j <= count; j++) {
                        Vec3d p = start.add(dir.multiply(j / density));
                        SecretRenderBackend.spawnParticle(p, effect);
                    }
                }
            }
        }

        // Target Secret
        if (step.targetSecret() != null) {
            renderSecret(step.targetSecret(), config, true);
        }
    }

    private static void renderComponents(List<BlockPos> relativeList, Color color, boolean filled, String label, boolean showText, float scale, SRMConfig.TextColor textColor, boolean enumToggle) {
        if (relativeList == null) return;
        int i = 1;
        for (BlockPos rel : relativeList) {
            BlockPos worldPos = toWorldPos(rel);
            Vec3d vec = new Vec3d(worldPos.getX(), worldPos.getY(), worldPos.getZ());
            SecretRenderBackend.addBox(vec, color, filled);
            if (showText) {
                String text = (textColor != null ? textColor.formatting : "") + label;
                if (enumToggle) text += " " + i;
                SecretRenderBackend.addText(text, vec.add(0.5, 0.5, 0.5), 0xFFFFFFFF, scale);
            }
            i++;
        }
    }

    private static void renderSecret(Secret secret, SRMConfig config, boolean isRouteTarget) {
        BlockPos worldPos = toWorldPos(secret.pos());
        Vec3d vec = new Vec3d(worldPos.getX(), worldPos.getY(), worldPos.getZ());
        Color c;
        boolean filled;
        String text = null;
        SRMConfig.TextColor textColor = SRMConfig.TextColor.WHITE;
        float textSize = 1.0f;
        boolean showText = false;

        switch (secret.category()) {
            case CHEST, WITHER -> {
                if (!config.renderSecretIteract && !isRouteTarget) return;
                c = config.secretsInteract;
                filled = config.secretsInteractFullBlock;
                showText = config.interactTextToggle;
                textColor = config.interactWaypointColor;
                textSize = config.interactTextSize;
                text = "Chest";
            }
            case BAT -> {
                if (!config.renderSecretBat && !isRouteTarget) return;
                c = config.secretsBat;
                filled = config.secretsBatFullBlock;
                showText = config.batTextToggle;
                textColor = config.batWaypointColor;
                textSize = config.batTextSize;
                text = "Bat";
            }
            case ITEM -> {
                if (!config.renderSecretsItem && !isRouteTarget) return;
                c = config.secretsItem;
                filled = config.secretsItemFullBlock;
                showText = config.itemTextToggle;
                textColor = config.itemWaypointColor;
                textSize = config.itemTextSize;
                text = "Item";
            }
            default -> { c = Color.RED; filled = false; }
        }

        SecretRenderBackend.addBox(vec, c, filled);
        if (showText && text != null) {
            SecretRenderBackend.addText(textColor.formatting + text, vec.add(0.5, 0.5, 0.5), 0xFFFFFFFF, textSize);
        }
    }

    private static void renderEnderPearls(RouteStep step, SRMConfig config) {
        List<Vec3d> pearls = step.enderpearls();
        List<List<Float>> angles = step.enderpearlangles();
        String roomDir = RoomDirectionUtils.roomDirection();
        Point roomCorner = RoomDirectionUtils.roomCorner();

        if (pearls == null) return;

        float yawOffset = switch (roomDir) {
            case "S" -> 0f;
            case "W" -> 90f;
            case "N" -> 180f;
            case "E" -> 270f;
            default -> 0f;
        };

        for (int i = 0; i < pearls.size(); i++) {
            Vec3d relativePearl = pearls.get(i);
            Triple<Double, Double, Double> worldTriple = RoomRotationUtils.relativeToActual(
                    relativePearl.x, relativePearl.y, relativePearl.z, roomDir, roomCorner
            );
            Vec3d worldPos = new Vec3d(worldTriple.getOne(), worldTriple.getTwo(), worldTriple.getThree());

            BlockPos blockContainer = new BlockPos((int)worldPos.x, (int)worldPos.y, (int)worldPos.z);
            Vec3d blockVec = new Vec3d(blockContainer.getX(), blockContainer.getY(), blockContainer.getZ());
            SecretRenderBackend.addBox(blockVec, config.enderpearls, config.enderpearlFullBlock);

            if (config.enderpearlTextToggle) {
                String label = config.enderpearlWaypointColor.formatting + "pearl";
                if (config.enderpearlEnumToggle) label += " " + (i + 1);
                SecretRenderBackend.addText(label, blockVec.add(0.5, 0.5, 0.5), 0xFFFFFFFF, config.enderpearlTextSize);
            }

            if (angles != null && i < angles.size()) {
                List<Float> angle = angles.get(i);
                float pitch = angle.get(0);
                float relativeYaw = angle.get(1) + 90;
                float realYaw = relativeYaw + yawOffset;

                double yawRad = Math.toRadians(realYaw);
                double pitchRad = Math.toRadians(pitch);

                double xDir = -Math.sin(yawRad) * Math.cos(pitchRad);
                double yDir = -Math.sin(pitchRad);
                double zDir = Math.cos(yawRad) * Math.cos(pitchRad);

                Vec3d trajectoryDir = new Vec3d(xDir, yDir, zDir);
                Vec3d startLine = worldPos.add(0, 1.62, 0);
                Vec3d endLine = startLine.add(trajectoryDir.multiply(5));

                SecretRenderBackend.addLine(startLine, endLine, config.pearlLineColor, config.pearlLineWidth);
            }
        }
    }

    private static BlockPos toWorldPos(BlockPos relative) {
        return RoomRotationUtils.relativeToActual(relative, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
    }

    private static Vec3d toWorldVec(Vec3d relative) {
        Triple<Double, Double, Double> t = RoomRotationUtils.relativeToActual(
                relative.x, relative.y, relative.z,
                RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner()
        );
        return new Vec3d(t.getOne(), t.getTwo(), t.getThree());
    }

    // AI generated mappings.
    private static ParticleEffect getParticle(SRMConfig.ParticleType type) {
        return switch (type) {
            case EXPLOSION_NORMAL -> ParticleTypes.EXPLOSION;
            case EXPLOSION_LARGE -> ParticleTypes.EXPLOSION_EMITTER;
            case EXPLOSION_HUGE -> ParticleTypes.EXPLOSION_EMITTER;
            case FIREWORKS_SPARK -> ParticleTypes.FIREWORK;
            case BUBBLE -> ParticleTypes.BUBBLE;
            case WATER_SPLASH -> ParticleTypes.SPLASH;
            case WATER_WAKE -> ParticleTypes.FISHING;
            case SUSPENDED -> ParticleTypes.UNDERWATER;
            case SUSPENDED_DEPTH -> ParticleTypes.UNDERWATER;
            case CRIT -> ParticleTypes.CRIT;
            case MAGIC_CRIT -> ParticleTypes.ENCHANTED_HIT;
            case SMOKE_NORMAL -> ParticleTypes.SMOKE;
            case SMOKE_LARGE -> ParticleTypes.LARGE_SMOKE;
            //case SPELL -> ParticleTypes.EFFECT;
            //case INSTANT_SPELL -> ParticleTypes.INSTANT_EFFECT;
            //case MOB_SPELL -> ParticleTypes.ENTITY_EFFECT;
            //case MOB_SPELL_AMBIENT -> ParticleTypes.AMBIENT_ENTITY_EFFECT;
            case WITCH_MAGIC -> ParticleTypes.WITCH;
            case DRIP_WATER -> ParticleTypes.DRIPPING_WATER;
            case DRIP_LAVA -> ParticleTypes.DRIPPING_LAVA;
            case VILLAGER_ANGRY -> ParticleTypes.ANGRY_VILLAGER;
            case VILLAGER_HAPPY -> ParticleTypes.HAPPY_VILLAGER;
            case TOWN_AURA -> ParticleTypes.MYCELIUM;
            case NOTE -> ParticleTypes.NOTE;
            case PORTAL -> ParticleTypes.PORTAL;
            case ENCHANTMENT_TABLE -> ParticleTypes.ENCHANT;
            case FLAME -> ParticleTypes.FLAME;
            case LAVA -> ParticleTypes.LAVA;
            case FOOTSTEP -> ParticleTypes.CLOUD;
            case CLOUD -> ParticleTypes.CLOUD;
            //case REDSTONE -> ParticleTypes.DUST;
            case SNOWBALL -> ParticleTypes.ITEM_SNOWBALL;
            case SNOW_SHOVEL -> ParticleTypes.ITEM_SNOWBALL;
            case SLIME -> ParticleTypes.ITEM_SLIME;
            case HEART -> ParticleTypes.HEART;
            // case BARRIER -> ParticleTypes.BLOCK_MARKER;
            case WATER_DROP -> ParticleTypes.RAIN;
            case ITEM_TAKE -> ParticleTypes.POOF;
            case MOB_APPEARANCE -> ParticleTypes.ELDER_GUARDIAN;
            default -> ParticleTypes.FLAME;
        };
    }
}