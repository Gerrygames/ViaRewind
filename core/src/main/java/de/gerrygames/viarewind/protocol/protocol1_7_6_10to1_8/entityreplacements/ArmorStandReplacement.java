package de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.entityreplacements;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_10Types;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10TO1_8;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.metadata.MetadataRewriter;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types.MetaType1_7_6_10;
import de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types.Types1_7_6_10;
import de.gerrygames.viarewind.replacement.EntityReplacement;
import de.gerrygames.viarewind.utils.PacketUtil;
import de.gerrygames.viarewind.utils.math.AABB;
import de.gerrygames.viarewind.utils.math.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class ArmorStandReplacement implements EntityReplacement {
	private int entityId;
	private List<Metadata> datawatcher = new ArrayList<>();
	private int[] entityIds = null;
	private double locX, locY, locZ;
	private State currentState = null;
	private boolean invisible = false;
	private boolean nameTagVisible = false;
	private String name = null;
	private UserConnection user;
	private float yaw, pitch;
	private float headYaw;
	private boolean small = false;
	private boolean marker = false;
	private static int ENTITY_ID = Integer.MAX_VALUE - 16000;

	public int getEntityId() {
		return this.entityId;
	}

	private enum State {
		HOLOGRAM, ZOMBIE;
	}

	public ArmorStandReplacement(int entityId, UserConnection user) {
		this.entityId = entityId;
		this.user = user;
	}

	public void setLocation(double x, double y, double z) {
		if (x != this.locX || y != this.locY || z != this.locZ) {
			this.locX = x;
			this.locY = y;
			this.locZ = z;
			updateLocation();
		}
	}

	public void relMove(double x, double y, double z) {
		if (x == 0.0 && y == 0.0 && z == 0.0) return;
		this.locX += x;
		this.locY += y;
		this.locZ += z;
		updateLocation();
	}

	public void setYawPitch(float yaw, float pitch) {
		if (this.yaw != yaw && this.pitch != pitch || this.headYaw != yaw) {
			this.yaw = yaw;
			this.headYaw = yaw;
			this.pitch = pitch;
			updateLocation();
		}
	}

	public void setHeadYaw(float yaw) {
		if (this.headYaw != yaw) {
			this.headYaw = yaw;
			updateLocation();
		}
	}

	public void updateMetadata(List<Metadata> metadataList) {
		for (Metadata metadata : metadataList) {
			datawatcher.removeIf(m -> m.id() == metadata.id());
			datawatcher.add(metadata);
		}
		updateState();
	}

	public void updateState() {
		byte flags = 0;
		byte armorStandFlags = 0;
		for (Metadata metadata : datawatcher) {
			if (metadata.id() == 0 && metadata.metaType() == MetaType1_8.Byte) {
				flags = (byte) metadata.getValue();
			} else if (metadata.id() == 2 && metadata.metaType() == MetaType1_8.String) {
				name = (String) metadata.getValue();
				if (name != null && name.equals("")) name = null;
			} else if (metadata.id() == 10 && metadata.metaType() == MetaType1_8.Byte) {
				armorStandFlags = (byte) metadata.getValue();
			} else if (metadata.id() == 3 && metadata.metaType() == MetaType1_8.Byte) {
				nameTagVisible = (byte) metadata.id() != 0;
			}
		}
		invisible = (flags & 0x20) != 0;
		small = (armorStandFlags & 0x01) != 0;
		marker = (armorStandFlags & 0x10) != 0;

		State prevState = currentState;
		if (invisible && name != null) {
			currentState = State.HOLOGRAM;
		} else {
			currentState = State.ZOMBIE;
		}

		if (currentState != prevState) {
			despawn();
			spawn();
		} else {
			updateMetadata();
			updateLocation();
		}
	}

	public void updateLocation() {
		if (entityIds == null) return;

		if (currentState == State.ZOMBIE) {
			PacketWrapper teleport = PacketWrapper.create(0x18, null, user);
			teleport.write(Type.INT, entityId);
			teleport.write(Type.INT, (int) (locX * 32.0));
			teleport.write(Type.INT, (int) (locY * 32.0));
			teleport.write(Type.INT, (int) (locZ * 32.0));
			teleport.write(Type.BYTE, (byte) ((yaw / 360f) * 256));
			teleport.write(Type.BYTE, (byte) ((pitch / 360f) * 256));

			PacketWrapper head = PacketWrapper.create(0x19, null, user);
			head.write(Type.INT, entityId);
			head.write(Type.BYTE, (byte) ((headYaw / 360f) * 256));

			PacketUtil.sendPacket(teleport, Protocol1_7_6_10TO1_8.class, true, true);
			PacketUtil.sendPacket(head, Protocol1_7_6_10TO1_8.class, true, true);
		} else if (currentState == State.HOLOGRAM) {
			PacketWrapper detach = PacketWrapper.create(0x1B, null, user);
			detach.write(Type.INT, entityIds[1]);
			detach.write(Type.INT, -1);
			detach.write(Type.BOOLEAN, false);

			PacketWrapper teleportSkull = PacketWrapper.create(0x18, null, user);
			teleportSkull.write(Type.INT, entityIds[0]);
			teleportSkull.write(Type.INT, (int) (locX * 32.0));
			teleportSkull.write(Type.INT, (int) ((locY + (marker ? 54.85 : small ? 56 : 57)) * 32.0));  //Don't ask me where this offset is coming from
			teleportSkull.write(Type.INT, (int) (locZ * 32.0));
			teleportSkull.write(Type.BYTE, (byte) 0);
			teleportSkull.write(Type.BYTE, (byte) 0);

			PacketWrapper teleportHorse = PacketWrapper.create(0x18, null, user);
			teleportHorse.write(Type.INT, entityIds[1]);
			teleportHorse.write(Type.INT, (int) (locX * 32.0));
			teleportHorse.write(Type.INT, (int) ((locY + 56.75) * 32.0));
			teleportHorse.write(Type.INT, (int) (locZ * 32.0));
			teleportHorse.write(Type.BYTE, (byte) 0);
			teleportHorse.write(Type.BYTE, (byte) 0);

			PacketWrapper attach = PacketWrapper.create(0x1B, null, user);
			attach.write(Type.INT, entityIds[1]);
			attach.write(Type.INT, entityIds[0]);
			attach.write(Type.BOOLEAN, false);

			PacketUtil.sendPacket(detach, Protocol1_7_6_10TO1_8.class, true, true);
			PacketUtil.sendPacket(teleportSkull, Protocol1_7_6_10TO1_8.class, true, true);
			PacketUtil.sendPacket(teleportHorse, Protocol1_7_6_10TO1_8.class, true, true);
			PacketUtil.sendPacket(attach, Protocol1_7_6_10TO1_8.class, true, true);
		}
	}

	public void updateMetadata() {
		if (entityIds == null) return;

		PacketWrapper metadataPacket = PacketWrapper.create(0x1C, null, user);

		if (currentState == State.ZOMBIE) {
			metadataPacket.write(Type.INT, entityIds[0]);

			List<Metadata> metadataList = new ArrayList<>();
			for (Metadata metadata : datawatcher) {
				if (metadata.id() < 0 || metadata.id() > 9) continue;
				metadataList.add(new Metadata(metadata.id(), metadata.metaType(), metadata.getValue()));
			}
			if (small) metadataList.add(new Metadata(12, MetaType1_8.Byte, (byte) 1));
			MetadataRewriter.transform(Entity1_10Types.EntityType.ZOMBIE, metadataList);

			metadataPacket.write(Types1_7_6_10.METADATA_LIST, metadataList);
		} else if (currentState == State.HOLOGRAM) {
			metadataPacket.write(Type.INT, entityIds[1]);

			List<Metadata> metadataList = new ArrayList<>();
			metadataList.add(new Metadata(12, MetaType1_7_6_10.Int, -1700000));
			metadataList.add(new Metadata(10, MetaType1_7_6_10.String, name));
			metadataList.add(new Metadata(11, MetaType1_7_6_10.Byte, (byte) 1));

			metadataPacket.write(Types1_7_6_10.METADATA_LIST, metadataList);
		} else {
			return;
		}

		PacketUtil.sendPacket(metadataPacket, Protocol1_7_6_10TO1_8.class);
	}

	public void spawn() {
		if (entityIds != null) despawn();

		if (currentState == State.ZOMBIE) {
			PacketWrapper spawn = PacketWrapper.create(0x0F, null, user);
			spawn.write(Type.VAR_INT, entityId);
			spawn.write(Type.UNSIGNED_BYTE, (short) 54);
			spawn.write(Type.INT, (int) (locX * 32.0));
			spawn.write(Type.INT, (int) (locY * 32.0));
			spawn.write(Type.INT, (int) (locZ * 32.0));
			spawn.write(Type.BYTE, (byte) 0);
			spawn.write(Type.BYTE, (byte) 0);
			spawn.write(Type.BYTE, (byte) 0);
			spawn.write(Type.SHORT, (short) 0);
			spawn.write(Type.SHORT, (short) 0);
			spawn.write(Type.SHORT, (short) 0);
			spawn.write(Types1_7_6_10.METADATA_LIST, new ArrayList<>());

			PacketUtil.sendPacket(spawn, Protocol1_7_6_10TO1_8.class, true, true);

			entityIds = new int[] {entityId};
		} else if (currentState == State.HOLOGRAM) {
			int[] entityIds = new int[] {entityId, ENTITY_ID--};

			PacketWrapper spawnSkull = PacketWrapper.create(0x0E, null, user);
			spawnSkull.write(Type.VAR_INT, entityIds[0]);
			spawnSkull.write(Type.BYTE, (byte) 66);
			spawnSkull.write(Type.INT, (int) (locX * 32.0));
			spawnSkull.write(Type.INT, (int) (locY * 32.0));
			spawnSkull.write(Type.INT, (int) (locZ * 32.0));
			spawnSkull.write(Type.BYTE, (byte) 0);
			spawnSkull.write(Type.BYTE, (byte) 0);
			spawnSkull.write(Type.INT, 0);

			PacketWrapper spawnHorse = PacketWrapper.create(0x0F, null, user);
			spawnHorse.write(Type.VAR_INT, entityIds[1]);
			spawnHorse.write(Type.UNSIGNED_BYTE, (short) 100);
			spawnHorse.write(Type.INT, (int) (locX * 32.0));
			spawnHorse.write(Type.INT, (int) (locY * 32.0));
			spawnHorse.write(Type.INT, (int) (locZ * 32.0));
			spawnHorse.write(Type.BYTE, (byte) 0);
			spawnHorse.write(Type.BYTE, (byte) 0);
			spawnHorse.write(Type.BYTE, (byte) 0);
			spawnHorse.write(Type.SHORT, (short) 0);
			spawnHorse.write(Type.SHORT, (short) 0);
			spawnHorse.write(Type.SHORT, (short) 0);
			spawnHorse.write(Types1_7_6_10.METADATA_LIST, new ArrayList<>());

			PacketUtil.sendPacket(spawnSkull, Protocol1_7_6_10TO1_8.class, true, true);
			PacketUtil.sendPacket(spawnHorse, Protocol1_7_6_10TO1_8.class, true, true);

			this.entityIds = entityIds;
		}

		updateMetadata();
		updateLocation();
	}

	public AABB getBoundingBox() {
		double w = this.small ? 0.25 : 0.5;
		double h = this.small ? 0.9875 : 1.975;
		Vector3d min = new Vector3d(this.locX - w / 2, this.locY, this.locZ - w / 2);
		Vector3d max = new Vector3d(this.locX + w / 2, this.locY + h, this.locZ + w / 2);
		return new AABB(min, max);
	}

	public void despawn() {
		if (entityIds == null) return;
		PacketWrapper despawn = PacketWrapper.create(0x13, null, user);
		despawn.write(Type.BYTE, (byte) entityIds.length);
		for (int id : entityIds) {
			despawn.write(Type.INT, id);
		}
		entityIds = null;
		PacketUtil.sendPacket(despawn, Protocol1_7_6_10TO1_8.class, true, true);
	}
}
