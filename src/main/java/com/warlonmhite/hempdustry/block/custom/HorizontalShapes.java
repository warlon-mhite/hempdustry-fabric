package com.warlonmhite.hempdustry.block.custom;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A facing block's outline in each horizontal direction, from the boxes it has facing north.
 *
 * <p>Turned a quarter at a time about the block's vertical centre line, clockwise seen from above —
 * the way a blockstate's {@code "y": 90} turns the model, so the outline stays on the drawing. A
 * point at (x, z) facing north sits at (16 - z, x) facing east. Newer versions ship this as
 * {@code Shapes.rotateHorizontal}; this line has nothing like it.
 */
public final class HorizontalShapes {
    private HorizontalShapes() {
    }

    /** {@code north} is a list of {x1, y1, z1, x2, y2, z2} boxes in pixels, as for {@code box}. */
    public static Map<Direction, VoxelShape> of(double[][] north) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : north) {
                double x1 = box[0], z1 = box[2], x2 = box[3], z2 = box[5];
                // Quarter turns clockwise from north: get2DDataValue() counts south 0, west 1, north 2, east 3.
                for (int turn = 0; turn < (facing.get2DDataValue() + 2) % 4; turn++) {
                    double nx1 = 16 - z2, nz1 = x1, nx2 = 16 - z1, nz2 = x2;
                    x1 = nx1; z1 = nz1; x2 = nx2; z2 = nz2;
                }
                shape = Shapes.or(shape, Block.box(x1, box[1], z1, x2, box[4], z2));
            }
            shapes.put(facing, shape);
        }
        return shapes;
    }
}
