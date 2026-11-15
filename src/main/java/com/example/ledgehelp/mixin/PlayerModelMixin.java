package com.example.ledgehelp.mixin;

import com.example.ledgehelp.PoseState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Рисует позы: висит на краю / тянет руку / тянет вверх / карабкается. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void ledgehelp$applyPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                     float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        int pose = PoseState.get(entity.getUUID());
        if (pose == PoseState.NONE) return;

        PlayerModel<?> m = (PlayerModel<?>) (Object) this;

        switch (pose) {
            case PoseState.HANG -> {
                // обе руки подняты и держатся за край, ноги слегка болтаются
                m.rightArm.xRot = -2.9F + Mth.sin(age * 0.08F) * 0.05F;
                m.rightArm.zRot = 0.15F;
                m.leftArm.xRot = -2.7F;
                m.leftArm.zRot = -0.15F;
                m.rightLeg.xRot = Mth.sin(age * 0.1F) * 0.12F;
                m.leftLeg.xRot = -Mth.sin(age * 0.1F) * 0.12F;
            }
            case PoseState.REACH -> {
                // помощник: рука вытянута вниз-вперёд
                m.rightArm.xRot = -0.95F + Mth.sin(age * 0.2F) * 0.05F;
                m.rightArm.yRot = 0.0F;
                m.rightArm.zRot = 0.0F;
                m.leftArm.xRot = -0.3F;
                m.body.xRot = 0.15F;
            }
            case PoseState.HAUL -> {
                // помощник: тянет двумя руками, откидываясь назад
                float pull = Mth.sin(age * 0.5F) * 0.3F;
                m.rightArm.xRot = -0.9F + pull;
                m.leftArm.xRot = -0.85F + pull;
                m.rightArm.zRot = 0.0F;
                m.leftArm.zRot = 0.0F;
                m.body.xRot = -0.25F;
            }
            case PoseState.CLIMB -> {
                // подтягиваемый: руки вверх, ногами перебирает по стене
                m.rightArm.xRot = -2.9F;
                m.rightArm.zRot = 0.1F;
                m.leftArm.xRot = -2.6F + Mth.sin(age * 0.5F) * 0.3F;
                m.leftArm.zRot = -0.1F;
                m.rightLeg.xRot = Mth.sin(age * 0.7F) * 0.7F;
                m.leftLeg.xRot = -Mth.sin(age * 0.7F) * 0.7F;
            }
            default -> { return; }
        }

        // setupAnim уже скопировал позы на слои одежды, поэтому копируем заново
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        m.jacket.copyFrom(m.body);
    }
}
