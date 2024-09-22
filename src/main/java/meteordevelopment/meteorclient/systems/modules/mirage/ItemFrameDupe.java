package meteordevelopment.meteorclient.systems.modules.mirage;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.player.AutoClicker;
import meteordevelopment.meteorclient.systems.modules.render.CameraTweaks;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class ItemFrameDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("The max distance to search for pistons.")
        .min(1)
        .sliderMin(1)
        .defaultValue(5)
        .sliderMax(6)
        .max(6)
        .build()
    );

    private final Setting<Boolean> backOfPiston = sgGeneral.add(new BoolSetting.Builder()
        .name("back-of-piston")
        .description("Whether to place on the front or back of piston")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between placements and breaking")
        .defaultValue(1)
        .sliderMax(10)
        .build()
    );


    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Whether or not to rotate when placing.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> rotateItem = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate item")
        .description("Whether or not to keep rotating the item frame")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Whether or not to swap back to the previous held item after placing.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("item-break-delay")
        .description("The amount of delay between breaking the item in ticks.")
        .defaultValue(2)
        .min(0)
        .sliderMax(60)
        .build()
    );

    public ItemFrameDupe() {
        super(Categories.MirageClient, "FrameDupe [M]", "Automatically places item frames on pistons (or not) and performs the item frame dupe");
    }

    private int timer;
    private final ArrayList<BlockPos> positions = new ArrayList<>();
    private static final ArrayList<BlockPos> blocks = new ArrayList<>();
    private Thread placeThread = null;
    private int breakDelaytimer;


    @Override
    public void onActivate() {
        timer = delay.get();
        breakDelaytimer = 0;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        placeThread = null;
        breakDelaytimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        ClientPlayerInteractionManager c = mc.interactionManager;
        assert mc.world != null;
        assert mc.player != null;

        if (timer > 0) {
            timer--;
            return;
        } else {
            timer = delay.get();
        }

        FindItemResult itemResult = InvUtils.findInHotbar(Items.ITEM_FRAME, Items.GLOW_ITEM_FRAME);
        if (!itemResult.found()) {
            error("No item frames found in hotbar.");
           toggle();
           return;
        }

        for (BlockPos blockPos : getSphere(mc.player.getBlockPos(), distance.get(), distance.get())) {
            if (mc.world.getBlockState(blockPos).getBlock() instanceof PistonBlock) {
                if (shouldPlace(blockPos)) positions.add(blockPos);
            }
        }

        for (BlockPos blockPos : positions) {
            assert mc.world != null;
            if( mc.world.getBlockState(blockPos).getBlock() == Blocks.AIR){
                continue;
            }
            Direction direction = mc.world.getBlockState(blockPos).get(FacingBlock.FACING);
            if (backOfPiston.get()) {
                direction = direction.getOpposite();
            }
            BlockPos placePos = getBlockPosFromDirection(direction, blockPos);
            BlockUtils.place(placePos, itemResult, rotate.get(), 50, true, true, swapBack.get());

            if (delay.get() != 0) {
                break;
            }
        }

        placeThread = new Thread(() -> {
            if(mc.player.getMainHandStack().getItem()==Items.ITEM_FRAME){
                return;
            }
                ItemFrameEntity itemFrame;
                Box box;
                box = new Box(mc.player.getEyePos().add(-3, -3, -3), mc.player.getEyePos().add(3, 3, 3));
                if (!mc.player.getWorld().getEntitiesByClass(ItemFrameEntity.class, box, itemFrameEntity -> true).isEmpty()) {
                    itemFrame = mc.player.getWorld().getEntitiesByClass(ItemFrameEntity.class, box, itemFrameEntity -> true).get(0);

                    assert c != null;
                    c.interactEntity(mc.player, itemFrame, Hand.MAIN_HAND);
                      if (itemFrame.getHeldItemStack().getCount() > 0) {
                        // Rotate the frame
                          if(rotateItem.get()) {
                              c.interactEntity(mc.player, itemFrame, Hand.MAIN_HAND);
                          }
                        // Delay before attacking the entity
                        try {
                            TimeUnit.MILLISECONDS.sleep(600);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                          breakDelaytimer++;
                          if (breakDelaytimer > breakDelay.get()) {
                              c.attackEntity(mc.player, itemFrame);
                              //Utils.leftClick();
                              breakDelaytimer = 0;
                          }
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
            }
        });
        placeThread.setName("PB-Thread");
        placeThread.start();
    }


    private boolean shouldPlace(BlockPos pistonPos) {
        Direction direction = mc.world.getBlockState(pistonPos).get(FacingBlock.FACING);
        if (backOfPiston.get()) {
            direction = direction.getOpposite();
        }
        BlockPos iFramePos = getBlockPosFromDirection(direction, pistonPos);

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity) {
                BlockPos entityPos = new BlockPos((int) Math.floor(entity.getPos().x), (int) Math.floor(entity.getPos().y), (int) Math.floor(entity.getPos().z));
                if (iFramePos.equals(entityPos)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        blocks.clear();

        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }

        return blocks;
    }

    private static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    private BlockPos getBlockPosFromDirection(Direction direction, BlockPos originalPos) {
        return switch (direction) {
            case UP -> originalPos.up();
            case DOWN -> originalPos.down();
            case EAST -> originalPos.east();
            case WEST -> originalPos.west();
            case NORTH -> originalPos.north();
            case SOUTH -> originalPos.south();
            default -> originalPos;
        };
    }
}
