package me.rufia.fightorflight.entity;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.function.Supplier;

public interface EntityFightOrFlight {
    DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(CobblemonFightOrFlight.MODID, Registries.ENTITY_TYPE);
    RegistrySupplier<EntityType<PokemonTracingBullet>> TRACING_BULLET = registerProjectile("tracing_bullet",
            EntityType.Builder.<PokemonTracingBullet>of(PokemonTracingBullet::new
                    , MobCategory.MISC).sized(0.3125f, 0.3125f));
    RegistrySupplier<EntityType<PokemonArrow>> ARROW_PROJECTILE = registerProjectile("arrow_projectile",
            EntityType.Builder.<PokemonArrow>of(PokemonArrow::new
                    , MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));
    RegistrySupplier<EntityType<PokemonBullet>> BULLET=registerProjectile("",
            EntityType.Builder.<PokemonBullet>of(PokemonBullet::new,MobCategory.MISC).sized(0.3125f,0.3125f));
    static void bootstrap() {
        ENTITY_TYPES.register();
    }

    static <T extends LivingEntity> RegistrySupplier<EntityType<T>> register(String name, EntityType.Builder<T> builder, Supplier<AttributeSupplier.Builder> attributes) {
        ResourceLocation id = new ResourceLocation(CobblemonFightOrFlight.MODID, name);
        return ENTITY_TYPES.register(id, () -> {
            EntityType<T> result = builder.build(id.toString());
            EntityAttributeRegistry.register(() -> result, attributes);

            return result;
        });
    }

    static <T extends Projectile> RegistrySupplier<EntityType<T>> registerProjectile(String name, EntityType.Builder<T> builder) {
        ResourceLocation id = new ResourceLocation(CobblemonFightOrFlight.MODID, name);
        return ENTITY_TYPES.register(id, () -> {
            EntityType<T> result = builder.build(id.toString());
            return result;
        });
    }
}
