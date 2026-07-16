package elocindev.spopaccompat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.Mod;
import net.spell_engine.internals.target.EntityRelations;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod(SpellEngineOpenPacCompat.MODID)
public final class SpellEngineOpenPacCompat {
    public static final String MODID = "spell_engine_openpac_compat";

    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static final AtomicBoolean OPAC_FAILURE_LOGGED = new AtomicBoolean();

    public SpellEngineOpenPacCompat() {
        EntityRelations.registerTeamMatcher("openpartiesandclaims", SpellEngineOpenPacCompat::getPartyRelation);
        LOGGER.info("Spell Engine x Open Parties and Claims compatibility enabled");
    }

    @Nullable
    private static EntityRelations.TeamRelation getPartyRelation(Entity attacker, Entity target) {
        if (!(attacker instanceof Player attackerPlayer)
                || !(target instanceof Player targetPlayer)
                || attackerPlayer.level().isClientSide()) {
            return null;
        }

        MinecraftServer server = attackerPlayer.getServer();
        if (server == null) {
            return null;
        }

        try {
            OpenPACServerAPI api = OpenPACServerAPI.get(server);
            if (api == null) {
                return null;
            }

            var partyManager = api.getPartyManager();
            var attackerParty = partyManager.getPartyByMember(attackerPlayer.getUUID());
            var targetParty = partyManager.getPartyByMember(targetPlayer.getUUID());
            if (attackerParty == null || targetParty == null) {
                return null;
            }

            var attackerPartyId = attackerParty.getId();
            var targetPartyId = targetParty.getId();
            if (attackerPartyId.equals(targetPartyId) || attackerParty.isAlly(targetPartyId)) {
                return new EntityRelations.TeamRelation(true, false);
            }
        } catch (RuntimeException exception) {
            if (OPAC_FAILURE_LOGGED.compareAndSet(false, true)) {
                LOGGER.warn("Failed to query Open Parties and Claims; falling back to Spell Engine relations", exception);
            }
        }

        return null;
    }
}
