package io.drahlek.dirigo.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlayerMovedEvent extends EventBase {
    private Player player;
    private BlockPos oldPos;
    private BlockPos newPos;
}
