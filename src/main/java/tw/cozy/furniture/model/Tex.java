package tw.cozy.furniture.model;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import tw.cozy.furniture.WoodType;

import java.util.function.Function;

/** 依木材決定零件要用哪種方塊外觀。 */
@FunctionalInterface
public interface Tex extends Function<WoodType, BlockData> {

    Tex PLANKS = w -> w.planks().createBlockData();
    Tex LOG = w -> axis(w.log(), Axis.Y);
    Tex LOG_X = w -> axis(w.log(), Axis.X);
    Tex LOG_Z = w -> axis(w.log(), Axis.Z);
    Tex STRIPPED = w -> axis(w.strippedLog(), Axis.Y);
    Tex STRIPPED_X = w -> axis(w.strippedLog(), Axis.X);
    Tex STRIPPED_Z = w -> axis(w.strippedLog(), Axis.Z);
    /** 布料（沙發、床單、燈罩），顏色依木材而定。 */
    Tex FABRIC = w -> w.fabric().createBlockData();
    Tex LEAVES = w -> {
        BlockData data = w.leaves().createBlockData();
        if (data instanceof Leaves leaves) {
            leaves.setPersistent(true);
        }
        return data;
    };

    static Tex fixed(Material material) {
        return w -> material.createBlockData();
    }

    private static BlockData axis(Material material, Axis axis) {
        BlockData data = material.createBlockData();
        if (data instanceof Orientable orientable) {
            orientable.setAxis(axis);
        }
        return data;
    }
}
