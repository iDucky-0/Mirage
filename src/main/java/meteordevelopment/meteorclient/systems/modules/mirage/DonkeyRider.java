package meteordevelopment.meteorclient.systems.modules.mirage;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

import static net.minecraft.stat.StatFormatter.DECIMAL_FORMAT;

public class DonkeyRider extends Module {
    private static boolean pressed = false;
    private static Vec3d firstPos;
    private static Vec3d finalPos;
    private final Setting<Double> yaw = settings.getDefaultGroup().add(new DoubleSetting.Builder()
        .name("Yaw")
        .description("Yaw of the player")
        .defaultValue(0.0f)
        .min(-180f)
        .max(180)
        .sliderMin(-180f)
        .sliderMax(180)
        .build()
    );
    private final Setting<Integer> distance = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("Distance")
        .description("Distance for the donkey to travel back and forth")
        .defaultValue(1001)
        .min(500)
        .max(2000)
        .sliderMin(500)
        .sliderMax(2000)
        .build()
    );
    private final Setting<Integer> waitBeforeMoving = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("Delay")
        .description("Wait Before Moving the donkey again after reaching the distance ")
        .defaultValue(1000)
        .min(500)
        .max(10000)
        .sliderMin(500)
        .sliderMax(10000)
        .build()
    );
    private final Setting<Boolean> remount = settings.getDefaultGroup().add(new BoolSetting.Builder()
        .name("Remount")
        .description("Remounts on the nearby donkey entity")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> RemountTime = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("Remount Delay")
        .description("Delay Before remounting again")
        .defaultValue(1000)
        .min(500)
        .max(10000)
        .sliderMin(500)
        .sliderMax(10000)
        .build()
    );
    private final Setting<Keybind> pause = settings.getDefaultGroup().add(new KeybindSetting.Builder()
        .name("Pause ")
        .description("Pauses or resumes when pressed")
        .defaultValue(Keybind.none())
        .build()
    );
    /* private final Setting<Boolean> AutoDisable = settings.getDefaultGroup().add(new BoolSetting.Builder()
         .name("AutoDisable")
         .description("Auto Disables the module when the player is not riding a donkey")
         .defaultValue(true)
         .build()
     );*/
    Thread runThread;
    private Vec3d currentPos;
    private boolean dismounted = false;
    private boolean remounted = false;
    private boolean yawChanged1 = false;
    private boolean yawChanged2 = false;
    private double YAW;
    private boolean running = true;

    public DonkeyRider() {
        super(Categories.MirageClient, "DonkeDupe [M]", "Riding the Donkey");
    }

    public static String getFirstPos() {
        return firstPos == null ? "" : firstPos.toString();
    }

    public static Vec3d getFirstPosVec() {
        return firstPos == null ? null : firstPos;
    }

    public static String getFinalPos() {
        return finalPos == null ? "" : finalPos.toString();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (mc.player != null)
            firstPos = mc.player.getPos();

        start();
    }

    public boolean shouldPause() {
        if (pause.get().isPressed())
            pressed = !pressed;
        return pressed;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        stop();
        setPressed(mc.options.forwardKey, false);
    }


    @SuppressWarnings("all")
    public void run() {
        if (runThread == null) {
            runThread = new Thread(() -> {
                while (running) {
                    if(mc.player==null && this.isActive()){
                        this.toggle();
                    }
                    YAW = yaw.get();
                    PlayerEntity player = mc.player;
                    if (player != null) {
                        if (player.getPos() != null) {
                            if (player.hasVehicle() && player.getVehicle() instanceof DonkeyEntity) {
                                currentPos = player.getPos();
                                //  mc.player.sendMessage(Text.of("Distance from starting position: " + DECIMAL_FORMAT.format(currentPos.distanceTo(firstPos)) + " blocks"), true);
                                if (currentPos != null && firstPos != null) {
                                    if (currentPos != null && currentPos.distanceTo(firstPos) >= distance.get() + 1 && !yawChanged1) {
                                        setPressed(mc.options.forwardKey, false);
                                        String AntiSpamText = "Reached!!! " + RandomStringUtils.randomAlphanumeric(11);
                                        try {
                                            ChatUtils.sendPlayerMsg(AntiSpamText);
                                            long startTime = System.currentTimeMillis();
                                            long endTime = startTime + waitBeforeMoving.get();
                                            while (System.currentTimeMillis() < endTime) {
                                                double timeLeft = (endTime - System.currentTimeMillis()) / 1000.0;
                                                mc.player.sendMessage(Text.of("Stopping for " + String.format("%.2f", timeLeft) + "s " + "because reached " + distance.get() + " blocks"), true);
                                                Thread.sleep(10);
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (yaw.get() >= 0) {
                                            yaw.set(yaw.get() - 180);
                                        } else {
                                            yaw.set(yaw.get() + 180);
                                        }
                                        YAW = yaw.get();
                                        finalPos = firstPos;
                                        firstPos = player.getPos();
                                        yaw.set(YAW);
                                        yawChanged1 = true;
                                        yawChanged2 = false;
                                        dismounted = false;
                                    }
                                    player.setYaw((float) YAW);
                                    if (running) {
                                        if (shouldPause())
                                            setPressed(mc.options.forwardKey, false);
                                        else
                                            setPressed(mc.options.forwardKey, isActive());
                                    }
                                    if (finalPos != null) {
                                        double pos = currentPos.distanceTo(finalPos);
                                        if (pos <= 1.5f && !dismounted) {
                                            setPressed(mc.options.forwardKey, false);
                                            player.dismountVehicle();
                                            ChatUtils.sendPlayerMsg(".dismount");
                                            mc.player.sendMessage(Text.of("Dismounted Succesfully"), false);
                                            setPressed(mc.options.forwardKey, true);
                                            dismounted = true;
                                            remounted = false;
                                            yawChanged1 = false;
                                            yawChanged2 = false;
                                            for(int i=0;i<200;i++){};
                                            setPressed(mc.options.forwardKey, false);
                                        }
                                    }
                                }
                            } else {
                                setPressed(mc.options.forwardKey, false);

                                if (dismounted && remount.get()) {
                                    List<DonkeyEntity> donkeys = player.getWorld().getEntitiesByClass(DonkeyEntity.class, player.getBoundingBox().expand(5), Entity -> !Entity.hasControllingPassenger());
                                    if (!donkeys.isEmpty() && !player.hasVehicle() && !remounted && !yawChanged2) {
                                        mc.player.sendMessage(Text.of("Attempting to remount"), false);
                                        try {
                                            long startTime = System.currentTimeMillis();
                                            long endTime = startTime + RemountTime.get()*2;
                                            while (System.currentTimeMillis() < endTime) {
                                                double timeLeft = (endTime - System.currentTimeMillis()) / 1000.0;
                                                mc.player.sendMessage(Text.of("Stopping for " + String.format("%.2f", timeLeft) + "s " + "because dismounted"), true);
                                                Thread.sleep(10);
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        if (player.distanceTo(donkeys.get(0)) > 4) {
                                            double xPos = Double.parseDouble(DECIMAL_FORMAT.format(donkeys.get(0).getBlockPos().getX()));
                                            double zPos = Double.parseDouble(DECIMAL_FORMAT.format(donkeys.get(0).getBlockPos().getZ()));
                                            double yPos = Double.parseDouble(DECIMAL_FORMAT.format(donkeys.get(0).getBlockPos().getY()));
                                            ChatUtils.sendPlayerMsg("#goto " + (xPos - 1) + " " + yPos + " " + (zPos - 1));
                                            try {
                                                Thread.sleep((long) (3000));
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            ChatUtils.sendPlayerMsg("#stop");
                                        }
                                        // player.startRiding(donkeys.get(0));
                                        //while (!player.hasVehicle() && player.distanceTo(donkeys.get(0))<3.5 && dismounted && !remounted) {
                                        mc.interactionManager.interactEntity(player, donkeys.get(0), Hand.MAIN_HAND);
                                        //}
                                        dismounted = false;
                                        finalPos = null;
                                        if (yaw.get() >= 0) {
                                            yaw.set(yaw.get() - 180);
                                        } else {
                                            yaw.set(yaw.get() + 180);
                                        }
                                        YAW = yaw.get();
                                        yawChanged2 = true;
                                        remounted = true;
                                        yawChanged1 = false;
                                        firstPos = player.getPos();
                                        mc.player.sendMessage(Text.of("Remount Successfull"), false);
                                    }
                                }
                            }
                        }
                    }
                }
            });
            runThread.setName("Donkey Rider Thread");
            runThread.start();
        }
    }

    public void stop() {
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }
        resetVariables();
    }

    public void start() {
        running = true;
        runThread = null;
        run();
    }


    public void resetVariables() {
        firstPos = null;
        finalPos = null;
        currentPos = null;
        running = false;
        runThread = null;
        dismounted = false;
        remounted = false;
        yawChanged1 = false;
        yawChanged2 = false;
        pressed = false;
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }
}
