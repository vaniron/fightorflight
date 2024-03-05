package me.rufia.fightorflight.goals;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.rufia.fightorflight.CobblemonFightOrFlight;
import me.rufia.fightorflight.PokemonInterface;
import me.rufia.fightorflight.entity.*;
import me.rufia.fightorflight.entity.projectile.AbstractPokemonProjectile;
import me.rufia.fightorflight.entity.projectile.PokemonArrow;
import me.rufia.fightorflight.entity.projectile.PokemonBullet;
import me.rufia.fightorflight.entity.projectile.PokemonTracingBullet;
import me.rufia.fightorflight.utils.PokemonUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

public class PokemonRangedAttackGoal extends Goal {
    public int ticksUntilNewAngerParticle = 0;

    public int ticksUntilNewAngerCry = 0;
    private final PokemonEntity pokemonEntity;
    private final LivingEntity livingEntity;
    @Nullable
    private LivingEntity target;
    private int attackTime;
    private final double speedModifier;
    private int seeTime;

    private final float attackRadius;
    private final float attackRadiusSqr;

    public PokemonRangedAttackGoal(LivingEntity pokemonEntity, double speedModifier, float attackRadius) {
        this.attackTime = -1;
        this.livingEntity = pokemonEntity;
        if (!(pokemonEntity instanceof PokemonEntity)) {
            throw new IllegalArgumentException("PokemonRangedAttackGoal requires a PokemonEntity");
        } else {
            this.pokemonEntity = (PokemonEntity) pokemonEntity;
            this.speedModifier = speedModifier;

            this.attackRadius = attackRadius;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
        }
    }

    public boolean canUse() {
        if (!CobblemonFightOrFlight.commonConfig().wild_pokemon_ranged_attack && pokemonEntity.getOwner() == null) {
            return false;
        }
        if (PokemonUtils.shouldMelee(pokemonEntity)) {
            return false;
        }

        LivingEntity livingEntity = this.pokemonEntity.getTarget();
        if (livingEntity != null && livingEntity.isAlive()) {
            this.target = livingEntity;
            return shouldFightTarget();
        } else {
            return false;
        }
    }

    public boolean canContinueToUse() {
        return this.canUse() || this.target.isAlive() && !this.pokemonEntity.getNavigation().isDone();
    }

    public void stop() {
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean shouldFightTarget() {
        //if (FightOrFlightCommonConfigs.DO_POKEMON_ATTACK.get() == false) { return false; }
        if (pokemonEntity.getPokemon().getLevel() < CobblemonFightOrFlight.commonConfig().minimum_attack_level) {
            return false;
        }

        LivingEntity owner = pokemonEntity.getOwner();
        if (owner != null) {
            if (!CobblemonFightOrFlight.commonConfig().do_pokemon_defend_owner) {
                return false;
            }
            if (this.pokemonEntity.getTarget() == null || this.pokemonEntity.getTarget() == owner) {
                return false;
            }

            if (this.pokemonEntity.getTarget() instanceof PokemonEntity targetPokemon) {
                LivingEntity targetOwner = targetPokemon.getOwner();
                if (targetOwner != null) {
                    if (targetOwner == owner) {
                        return false;
                    }
                    if (!CobblemonFightOrFlight.commonConfig().do_player_pokemon_attack_other_player_pokemon) {
                        return false;
                    }
                }
            }
            if (this.pokemonEntity.getTarget() instanceof Player) {
                if (!CobblemonFightOrFlight.commonConfig().do_player_pokemon_attack_other_players) {
                    return false;
                }
            }

        } else {
            if (this.pokemonEntity.getTarget() != null) {
                if (CobblemonFightOrFlight.getFightOrFlightCoefficient(pokemonEntity) <= 0) {
                    return false;
                }

                LivingEntity targetEntity = this.pokemonEntity.getTarget();
                if (this.pokemonEntity.distanceToSqr(targetEntity.getX(), targetEntity.getY(), targetEntity.getZ()) > 400) {
                    return false;
                }
            }
        }
        //if (pokemonEntity.getPokemon().isPlayerOwned()) { return false; }

        return !pokemonEntity.isBusy();
    }

    public boolean isTargetInBattle() {
        if (this.pokemonEntity.getTarget() instanceof ServerPlayer targetAsPlayer) {
            return BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(targetAsPlayer) != null;
        }
        return false;
    }

    public void tick() {
        if (!CobblemonFightOrFlight.commonConfig().do_pokemon_attack_in_battle) {
            if (isTargetInBattle()) {
                this.pokemonEntity.getNavigation().setSpeedModifier(0);
            }
        }

        if (pokemonEntity.getOwner() == null) {
            if (ticksUntilNewAngerParticle < 1) {
                CobblemonFightOrFlight.PokemonEmoteAngry((Mob) this.livingEntity);
                ticksUntilNewAngerParticle = 10;
            } else {
                ticksUntilNewAngerParticle = ticksUntilNewAngerParticle - 1;
            }

            if (ticksUntilNewAngerCry < 1) {
                pokemonEntity.cry();
                ticksUntilNewAngerCry = 100 + (int) (Math.random() * 200);
            } else {
                ticksUntilNewAngerCry = ticksUntilNewAngerCry - 1;
            }
        }
        double d = this.pokemonEntity.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean bl = this.pokemonEntity.getSensing().hasLineOfSight(this.target);
        if (bl) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (!(d > (double) this.attackRadiusSqr) && this.seeTime >= 5) {
            this.pokemonEntity.getNavigation().stop();
        } else {
            this.pokemonEntity.getNavigation().moveTo(this.target, this.speedModifier);
        }

        this.pokemonEntity.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        --this.attackTime;
        ((PokemonInterface) (Object) pokemonEntity).setAttackTime(((PokemonInterface) (Object) pokemonEntity).getAttackTime() + 1);
        if (this.attackTime == 0) {
            if (!bl) {
                return;
            }
            float speedModifier = Math.max(0.1f, 1 - this.pokemonEntity.getSpeed() / CobblemonFightOrFlight.commonConfig().speed_stat_limit);
            float f = (float) Math.sqrt(d) / this.attackRadius * speedModifier;
            //float g = Mth.clamp(f, 0.1F, 1.0F);
            this.performRangedAttack(this.target);
            ((PokemonInterface) (Object) pokemonEntity).setAttackTime(0);
            this.attackTime = Mth.floor(20 * Mth.lerp(f, CobblemonFightOrFlight.commonConfig().minimum_ranged_attack_interval, CobblemonFightOrFlight.commonConfig().maximum_ranged_attack_interval));
        } else if (this.attackTime < 0) {
            this.attackTime = Mth.floor(Mth.lerp(Math.sqrt(d) / (double) this.attackRadius, 20 * CobblemonFightOrFlight.commonConfig().minimum_ranged_attack_interval, 20 * CobblemonFightOrFlight.commonConfig().maximum_ranged_attack_interval));
        }
    }

    public void addProjectileEntity(AbstractPokemonProjectile projectile, Move move) {
        projectile.setElementalType(move.getType().getName());
        projectile.setDamage(PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, true, (float) move.getPower()));
        this.livingEntity.level().addFreshEntity(projectile);
    }

    public void addProjectileEntity(AbstractPokemonProjectile projectile) {
        projectile.setElementalType(pokemonEntity.getPokemon().getPrimaryType().getName());
        projectile.setDamage(PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, true));
        this.livingEntity.level().addFreshEntity(projectile);
    }

    public void performRangedAttack(LivingEntity target) {
        Move move = PokemonUtils.getMove(pokemonEntity, true);
        AbstractPokemonProjectile bullet;

        if (move != null) {
            String moveName = move.getName();
            CobblemonFightOrFlight.LOGGER.info(moveName);
            Random rand = new Random();
            boolean b1 = Arrays.stream(CobblemonFightOrFlight.moveConfig().single_bullet_moves).toList().contains(moveName);
            boolean b2 = Arrays.stream(CobblemonFightOrFlight.moveConfig().multiple_bullet_moves).toList().contains(moveName);
            boolean b3 = Arrays.stream(CobblemonFightOrFlight.moveConfig().single_tracing_bullet_moves).toList().contains(moveName);
            boolean b4 = Arrays.stream(CobblemonFightOrFlight.moveConfig().multiple_tracing_bullet_moves).toList().contains(moveName);
            boolean b5 = Arrays.stream(CobblemonFightOrFlight.moveConfig().single_beam_moves).toList().contains(moveName);
            if (b3 || b4) {
                for (int i = 0; i < (b3 ? 1 : rand.nextInt(3) + 1); ++i) {
                    bullet = new PokemonTracingBullet(livingEntity.level(), pokemonEntity, target, livingEntity.getDirection().getAxis());
                    addProjectileEntity(bullet, move);
                }
            } else if (b1 || b2) {
                for (int i = 0; i < (b1 ? 1 : rand.nextInt(3) + 1); ++i) {
                    bullet = new PokemonBullet(livingEntity.level(), pokemonEntity, target);
                    double d = target.getX() - this.livingEntity.getX();
                    double e = target.getY(0.3333333333333333) - bullet.getY();
                    double f = target.getZ() - this.livingEntity.getZ();
                    double g = Math.sqrt(d * d + f * f);
                    bullet.shoot(d, e + g * 0.2, f, 1.6F, 0.1f);
                    addProjectileEntity(bullet, move);
                }
            } else if (b5) {
                target.hurt(pokemonEntity.damageSources().mobAttack(pokemonEntity), PokemonAttackEffect.calculatePokemonDamage(pokemonEntity, true, (float) move.getPower()));
            } else {
                bullet = new PokemonArrow(livingEntity.level(), pokemonEntity, target);
                double d = target.getX() - this.livingEntity.getX();
                double e = target.getY(0.3333333333333333) - bullet.getY();
                double f = target.getZ() - this.livingEntity.getZ();
                double g = Math.sqrt(d * d + f * f);
                bullet.shoot(d, e + g * 0.2, f, 1.6F, 0.1f);
                addProjectileEntity(bullet, move);
            }
        } else {
            bullet = new PokemonArrow(livingEntity.level(), pokemonEntity, target);
            double d = target.getX() - this.livingEntity.getX();
            double e = target.getY(0.3333333333333333) - bullet.getY();
            double f = target.getZ() - this.livingEntity.getZ();
            double g = Math.sqrt(d * d + f * f);
            bullet.shoot(d, e + g * 0.2, f, 1.6F, 0.1f);
            addProjectileEntity(bullet);
        }
        PokemonAttackEffect.applyPostEffect(pokemonEntity,target,move);
    }
}
