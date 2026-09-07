package ch.njol.unofficialmonumentamod.compat;

import com.provismet.provihealth.api.ProviHealthApi;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class HealthBarHook implements ProviHealthApi {
    @Override
    public void onInitialize () {
        final Identifier DUELIST_BORDER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/gui/healthbars/duelist_portrait.png");
        final Identifier SLAYER_BORDER = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/gui/healthbars/slayer_portrait.png");

        // Creates new displays for Monumenta's custom entity groupings.
        final EntityType<?>[] duelist = {
            EntityType.VEX,
            EntityType.WITCH,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.IRON_GOLEM,
            EntityType.GIANT,
            EntityType.ALLAY,
            EntityType.WARDEN
        };

        final EntityType<?>[] slayer = {
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.SLIME,
            EntityType.MAGMA_CUBE,
            EntityType.SHULKER,
            EntityType.WOLF,
            EntityType.RAVAGER,
            EntityType.HOGLIN,
            EntityType.COW,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.CHICKEN
        };

        // Duelist
        this.registerPortrait(EntityGroup.ILLAGER, DUELIST_BORDER, true);
        this.registerIcon(EntityGroup.ILLAGER, Items.PLAYER_HEAD, true);
        for (EntityType<?> type : duelist) {
            this.registerPortrait(type, DUELIST_BORDER);
            this.registerIcon(type, Items.PLAYER_HEAD);
        }

        // Slayer
        this.registerPortrait(EntityGroup.AQUATIC, SLAYER_BORDER, true);
        this.registerPortrait(EntityGroup.ARTHROPOD, SLAYER_BORDER, true);
        this.registerIcon(EntityGroup.AQUATIC, Items.CREEPER_HEAD, true);
        this.registerIcon(EntityGroup.ARTHROPOD, Items.CREEPER_HEAD, true);
        for (EntityType<?> type : slayer) {
            this.registerPortrait(type, SLAYER_BORDER);
            this.registerIcon(type, Items.CREEPER_HEAD);
        }
    }
}
