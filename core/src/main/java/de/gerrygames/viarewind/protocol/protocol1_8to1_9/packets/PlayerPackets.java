package de.gerrygames.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_10Types;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_8;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_8;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.Protocol1_8TO1_9;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.items.ItemRewriter;
import de.gerrygames.viarewind.protocol.protocol1_8to1_9.storage.*;
import de.gerrygames.viarewind.utils.ChatUtil;
import de.gerrygames.viarewind.utils.PacketUtil;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerPackets {

	public static void register(Protocol<ClientboundPackets1_9, ClientboundPackets1_8,
			ServerboundPackets1_9, ServerboundPackets1_8> protocol) {
		/*  OUTGOING  */

		//Animation
		//Statistics

		//Boss Bar
		protocol.registerClientbound(ClientboundPackets1_9.BOSSBAR, null, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(packetWrapper -> {
					packetWrapper.cancel();

					UUID uuid = packetWrapper.read(Type.UUID);
					int action = packetWrapper.read(Type.VAR_INT);
					BossBarStorage bossBarStorage = packetWrapper.user().get(BossBarStorage.class);
					if (action == 0) {
						bossBarStorage.add(uuid, ChatUtil.jsonToLegacy(packetWrapper.read(Type.COMPONENT)), packetWrapper.read(Type.FLOAT));
						packetWrapper.read(Type.VAR_INT);
						packetWrapper.read(Type.VAR_INT);
						packetWrapper.read(Type.UNSIGNED_BYTE);
					} else if (action == 1) {
						bossBarStorage.remove(uuid);
					} else if (action == 2) {
						bossBarStorage.updateHealth(uuid, packetWrapper.read(Type.FLOAT));
					} else if (action == 3) {
						String title = ChatUtil.jsonToLegacy(packetWrapper.read(Type.COMPONENT));
						bossBarStorage.updateTitle(uuid, title);
					}
				});
			}
		});

		//Tab-Complete
		//Chat Message

		//Set Cooldown
		protocol.cancelClientbound(ClientboundPackets1_9.COOLDOWN);

		//Custom Payload
		protocol.registerClientbound(ClientboundPackets1_9.PLUGIN_MESSAGE, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(packetWrapper -> {
					String channel = packetWrapper.get(Type.STRING, 0);
					if (channel.equalsIgnoreCase("MC|TrList")) {
						packetWrapper.passthrough(Type.INT);  //Window Id

						int size;
						if (packetWrapper.isReadable(Type.BYTE, 0)) {
							size = packetWrapper.passthrough(Type.BYTE);
						} else {
							size = packetWrapper.passthrough(Type.UNSIGNED_BYTE);
						}

						for (int i = 0; i < size; i++) {
							packetWrapper.write(Type.ITEM, ItemRewriter.toClient(packetWrapper.read(Type.ITEM))); //Buy Item 1
							packetWrapper.write(Type.ITEM, ItemRewriter.toClient(packetWrapper.read(Type.ITEM))); //Buy Item 3

							boolean has3Items = packetWrapper.passthrough(Type.BOOLEAN);
							if (has3Items) {
								packetWrapper.write(Type.ITEM, ItemRewriter.toClient(packetWrapper.read(Type.ITEM))); //Buy Item 2
							}

							packetWrapper.passthrough(Type.BOOLEAN); //Unavailable
							packetWrapper.passthrough(Type.INT); //Uses
							packetWrapper.passthrough(Type.INT); //Max Uses
						}
					} else if (channel.equalsIgnoreCase("MC|BOpen")) {
						packetWrapper.read(Type.VAR_INT);
					}
				});
			}
		});

		//Disconnect
		//Change Game State
		protocol.registerClientbound(ClientboundPackets1_9.GAME_EVENT, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.UNSIGNED_BYTE);
				map(Type.FLOAT);
				handler(packetWrapper -> {
					int reason = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
					if (reason == 3)
						packetWrapper.user().get(EntityTracker.class).setPlayerGamemode(packetWrapper.get(Type.FLOAT, 0).intValue());
				});
			}
		});

		//Join Game
		protocol.registerClientbound(ClientboundPackets1_9.JOIN_GAME, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.UNSIGNED_BYTE);
				map(Type.BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.STRING);
				map(Type.BOOLEAN);
				handler(packetWrapper -> {
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.setPlayerId(packetWrapper.get(Type.INT, 0));
					tracker.setPlayerGamemode(packetWrapper.get(Type.UNSIGNED_BYTE, 0));
					tracker.getClientEntityTypes().put(tracker.getPlayerId(), Entity1_10Types.EntityType.ENTITY_HUMAN);
				});
				handler(packetWrapper -> {
					ClientWorld world = packetWrapper.user().get(ClientWorld.class);
					world.setEnvironment(packetWrapper.get(Type.BYTE, 0));
				});
			}
		});

		//Open Sign Editor
		//Player Abilities
		//Player List Item

		//Player Position And Look
		protocol.registerClientbound(ClientboundPackets1_9.PLAYER_POSITION, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BYTE);
				handler(packetWrapper -> {
					PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);

					int teleportId = packetWrapper.read(Type.VAR_INT);
					pos.setConfirmId(teleportId);

					byte flags = packetWrapper.get(Type.BYTE, 0);
					double x = packetWrapper.get(Type.DOUBLE, 0);
					double y = packetWrapper.get(Type.DOUBLE, 1);
					double z = packetWrapper.get(Type.DOUBLE, 2);
					float yaw = packetWrapper.get(Type.FLOAT, 0);
					float pitch = packetWrapper.get(Type.FLOAT, 1);

					packetWrapper.set(Type.BYTE, 0, (byte) 0);

					if (flags != 0) {
						if ((flags & 0x01) != 0) {
							x += pos.getPosX();
							packetWrapper.set(Type.DOUBLE, 0, x);
						}
						if ((flags & 0x02) != 0) {
							y += pos.getPosY();
							packetWrapper.set(Type.DOUBLE, 1, y);
						}
						if ((flags & 0x04) != 0) {
							z += pos.getPosZ();
							packetWrapper.set(Type.DOUBLE, 2, z);
						}
						if ((flags & 0x08) != 0) {
							yaw += pos.getYaw();
							packetWrapper.set(Type.FLOAT, 0, yaw);
						}
						if ((flags & 0x10) != 0) {
							pitch += pos.getPitch();
							packetWrapper.set(Type.FLOAT, 1, pitch);
						}
					}

					pos.setPos(x, y, z);
					pos.setYaw(yaw);
					pos.setPitch(pitch);
				});
			}
		});

		//Resource Pack Send

		//Respawn
		protocol.registerClientbound(ClientboundPackets1_9.RESPAWN, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.INT);
				map(Type.UNSIGNED_BYTE);
				map(Type.UNSIGNED_BYTE);
				map(Type.STRING);
				handler(packetWrapper -> packetWrapper.user().get(EntityTracker.class).setPlayerGamemode(packetWrapper.get(Type.UNSIGNED_BYTE, 1)));
				handler(packetWrapper -> {
					packetWrapper.user().get(BossBarStorage.class).updateLocation();
					packetWrapper.user().get(BossBarStorage.class).changeWorld();
				});
				handler(packetWrapper -> {
					ClientWorld world = packetWrapper.user().get(ClientWorld.class);
					world.setEnvironment(packetWrapper.get(Type.INT, 0));
				});
			}
		});

		//Camera
		//Held Item Change
		//Set Experience
		//Update Health
		//Spawn Position
		//Title
		//Player List Header And Footer

		/*  INCMOING  */

		//Chat Message
		protocol.registerServerbound(ServerboundPackets1_8.CHAT_MESSAGE, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(packetWrapper -> {
					String msg = packetWrapper.get(Type.STRING, 0);
					if (msg.toLowerCase().startsWith("/offhand")) {
						packetWrapper.cancel();
						PacketWrapper swapItems = PacketWrapper.create(0x13, null, packetWrapper.user());
						swapItems.write(Type.VAR_INT, 6);
						swapItems.write(Type.POSITION, new Position(0, (short) 0, 0));
						swapItems.write(Type.BYTE, (byte) 255);

						PacketUtil.sendToServer(swapItems, Protocol1_8TO1_9.class, true, true);
					}
				});
			}
		});

		//Confirm Transaction

		//Use Entity
		protocol.registerServerbound(ServerboundPackets1_8.INTERACT_ENTITY, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				handler(packetWrapper -> {
					int type = packetWrapper.get(Type.VAR_INT, 1);
					if (type == 2) {
						packetWrapper.passthrough(Type.FLOAT);
						packetWrapper.passthrough(Type.FLOAT);
						packetWrapper.passthrough(Type.FLOAT);
					}
					if (type == 2 || type == 0) {
						packetWrapper.write(Type.VAR_INT, 0);
					}
				});
			}
		});

		//Player
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_MOVEMENT, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.BOOLEAN);
				handler(packetWrapper -> {
					//Sending any queued animations.
					PacketWrapper animation = null;
					while((animation = ((Protocol1_8TO1_9)protocol).animationsToSend.poll()) != null) {
						PacketUtil.sendToServer(animation, Protocol1_8TO1_9.class, true, true);
					}

					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					int playerId = tracker.getPlayerId();
					if (tracker.isInsideVehicle(playerId)) packetWrapper.cancel();
				});
			}
		});

		//Player Position
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_POSITION, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.BOOLEAN);
				handler(packetWrapper -> {
					//Sending any queued animations.
					PacketWrapper animation = null;
					while((animation = ((Protocol1_8TO1_9)protocol).animationsToSend.poll()) != null) {
						PacketUtil.sendToServer(animation, Protocol1_8TO1_9.class,
								true, true);
					}

					PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
					if (pos.getConfirmId() != -1) return;
					pos.setPos(packetWrapper.get(Type.DOUBLE, 0), packetWrapper.get(Type.DOUBLE, 1),
							packetWrapper.get(Type.DOUBLE, 2));
					pos.setOnGround(packetWrapper.get(Type.BOOLEAN, 0));
				});
				handler(packetWrapper -> packetWrapper.user().get(BossBarStorage.class).updateLocation());
			}
		});

		//Player Look
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_ROTATION, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BOOLEAN);
				handler(packetWrapper -> {
					//Sending any queued animations.
					PacketWrapper animation = null;
					while((animation = ((Protocol1_8TO1_9)protocol).animationsToSend.poll()) != null) {
						PacketUtil.sendToServer(animation, Protocol1_8TO1_9.class, true, true);
					}

					PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
					if (pos.getConfirmId() != -1) return;
					pos.setYaw(packetWrapper.get(Type.FLOAT, 0));
					pos.setPitch(packetWrapper.get(Type.FLOAT, 1));
					pos.setOnGround(packetWrapper.get(Type.BOOLEAN, 0));
				});
				handler(packetWrapper -> packetWrapper.user().get(BossBarStorage.class).updateLocation());
			}
		});

		//Player Position And Look
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_POSITION_AND_ROTATION, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.DOUBLE);
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.BOOLEAN);
				handler(packetWrapper -> {
					//Sending any queued animations.
					PacketWrapper animation = null;
					while((animation = ((Protocol1_8TO1_9)protocol).animationsToSend.poll()) != null) {
						PacketUtil.sendToServer(animation, Protocol1_8TO1_9.class, true, true);
					}

					double x = packetWrapper.get(Type.DOUBLE, 0);
					double y = packetWrapper.get(Type.DOUBLE, 1);
					double z = packetWrapper.get(Type.DOUBLE, 2);
					float yaw = packetWrapper.get(Type.FLOAT, 0);
					float pitch = packetWrapper.get(Type.FLOAT, 1);
					boolean onGround = packetWrapper.get(Type.BOOLEAN, 0);

					PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
					if (pos.getConfirmId() != -1) {
						if (pos.getPosX() == x && pos.getPosY() == y && pos.getPosZ() == z
								&& pos.getYaw() == yaw && pos.getPitch() == pitch) {
							PacketWrapper confirmTeleport = packetWrapper.create(0x00);
							confirmTeleport.write(Type.VAR_INT, pos.getConfirmId());
							PacketUtil.sendToServer(confirmTeleport,
									Protocol1_8TO1_9.class, true, true);

							pos.setConfirmId(-1);
						}
					} else {
						pos.setPos(x, y, z);
						pos.setYaw(yaw);
						pos.setPitch(pitch);
						pos.setOnGround(onGround);
						packetWrapper.user().get(BossBarStorage.class).updateLocation();
					}
				});
			}
		});

		//Player Digging
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_DIGGING, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.BYTE, Type.VAR_INT);
				map(Type.POSITION);
				map(Type.BYTE);
				handler(packetWrapper -> {
					int state = packetWrapper.get(Type.VAR_INT, 0);
					if (state == 0) {
						packetWrapper.user().get(BlockPlaceDestroyTracker.class).setMining(true);
					} else if (state == 2) {
						BlockPlaceDestroyTracker tracker = packetWrapper.user().get(BlockPlaceDestroyTracker.class);
						tracker.setMining(false);
						tracker.setLastMining(System.currentTimeMillis() + 100);
						packetWrapper.user().get(Cooldown.class).setLastHit(0);
					} else if (state == 1) {
						BlockPlaceDestroyTracker tracker = packetWrapper.user().get(BlockPlaceDestroyTracker.class);
						tracker.setMining(false);
						tracker.setLastMining(0);
						packetWrapper.user().get(Cooldown.class).hit();
					}
				});
			}
		});

		//Player Block Placement
		protocol.registerServerbound(ServerboundPackets1_8.PLAYER_BLOCK_PLACEMENT, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				map(Type.BYTE, Type.VAR_INT);
				map(Type.ITEM, Type.NOTHING);
				create(Type.VAR_INT, 0); //Main Hand
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				handler(packetWrapper -> {
					if (packetWrapper.get(Type.VAR_INT, 0) == -1) {
						packetWrapper.cancel();
						PacketWrapper useItem = PacketWrapper.create(0x1D, null, packetWrapper.user());
						useItem.write(Type.VAR_INT, 0);

						PacketUtil.sendToServer(useItem, Protocol1_8TO1_9.class, true, true);
					}
				});
				handler(packetWrapper -> {
					if (packetWrapper.get(Type.VAR_INT, 0) != -1) {
						packetWrapper.user().get(BlockPlaceDestroyTracker.class).place();
					}
				});
			}
		});

		//Held Item Change
		protocol.registerServerbound(ServerboundPackets1_8.HELD_ITEM_CHANGE, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(packetWrapper -> packetWrapper.user().get(Cooldown.class).hit());
			}
		});

		//Animation
		protocol.registerServerbound(ServerboundPackets1_8.ANIMATION, new PacketRemapper() {
			@Override
			public void registerMap() {
				handler(packetWrapper -> {
					packetWrapper.cancel();

					/* We have to add ArmAnimation to a queue to be sent on PacketPlayInFlying. In 1.9,
					 * PacketPlayInArmAnimation is sent after PacketPlayInUseEntity, not before like it used to be.
					 * However, all packets are sent before PacketPlayInFlying. We'd just do a normal delay, but
					 * it would cause the packet to be sent after PacketPlayInFlying, potentially false flagging
					 * anticheats that check for this behavior from clients. Since all packets are sent before
					 * PacketPlayInFlying, if we queue it to be sent right before PacketPlayInFlying is processed,
					 * we can be certain it will be sent after PacketPlayInUseEntity */
					packetWrapper.cancel();
					final PacketWrapper delayedPacket = PacketWrapper.create(0x1A,
							null, packetWrapper.user());
					delayedPacket.write(Type.VAR_INT, 0);  //Main Hand

					((Protocol1_8TO1_9)protocol).animationsToSend.add(delayedPacket);
				});
				handler(packetWrapper -> {
					packetWrapper.user().get(BlockPlaceDestroyTracker.class).updateMining();
					packetWrapper.user().get(Cooldown.class).hit();
				});
			}
		});

		//Entity Action
		protocol.registerServerbound(ServerboundPackets1_8.ENTITY_ACTION, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				map(Type.VAR_INT);
				handler(packetWrapper -> {
					int action = packetWrapper.get(Type.VAR_INT, 1);
					if (action == 6) {
						packetWrapper.set(Type.VAR_INT, 1, 7);
					} else if (action == 0) {
						PlayerPosition pos = packetWrapper.user().get(PlayerPosition.class);
						if (!pos.isOnGround()) {
							PacketWrapper elytra = PacketWrapper.create(0x14, null, packetWrapper.user());
							elytra.write(Type.VAR_INT, packetWrapper.get(Type.VAR_INT, 0));
							elytra.write(Type.VAR_INT, 8);
							elytra.write(Type.VAR_INT, 0);
							PacketUtil.sendToServer(elytra, Protocol1_8TO1_9.class, true, false);
						}
					}
				});
			}
		});

		//Steer Vehicle
		protocol.registerServerbound(ServerboundPackets1_8.STEER_VEHICLE, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.FLOAT);
				map(Type.FLOAT);
				map(Type.UNSIGNED_BYTE);
				handler(packetWrapper -> {
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					int playerId = tracker.getPlayerId();
					int vehicle = tracker.getVehicle(playerId);
					if (vehicle != -1 && tracker.getClientEntityTypes().get(vehicle) == Entity1_10Types.EntityType.BOAT) {
						PacketWrapper steerBoat = PacketWrapper.create(0x11, null, packetWrapper.user());
						float left = packetWrapper.get(Type.FLOAT, 0);
						float forward = packetWrapper.get(Type.FLOAT, 1);
						steerBoat.write(Type.BOOLEAN, forward != 0.0f || left < 0.0f);
						steerBoat.write(Type.BOOLEAN, forward != 0.0f || left > 0.0f);
						PacketUtil.sendToServer(steerBoat, Protocol1_8TO1_9.class);
					}
				});
			}
		});

		//Update Sign
		protocol.registerServerbound(ServerboundPackets1_8.UPDATE_SIGN, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.POSITION);
				handler(packetWrapper -> {
					for (int i = 0; i < 4; i++) {
						packetWrapper.write(Type.STRING, ChatUtil.jsonToLegacy(packetWrapper.read(Type.COMPONENT)));
					}
				});
			}
		});

		//Player Abilities

		//Tab Complete
		protocol.registerServerbound(ServerboundPackets1_8.TAB_COMPLETE, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(packetWrapper -> packetWrapper.write(Type.BOOLEAN, false));
				map(Type.OPTIONAL_POSITION);
			}
		});

		//Client Settings
		protocol.registerServerbound(ServerboundPackets1_8.CLIENT_SETTINGS, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				map(Type.BYTE, Type.VAR_INT);
				map(Type.BOOLEAN);
				map(Type.UNSIGNED_BYTE);
				create(Type.VAR_INT, 1);
				handler(packetWrapper -> {
					short flags = packetWrapper.get(Type.UNSIGNED_BYTE, 0);

					PacketWrapper updateSkin = PacketWrapper.create(0x1C, null, packetWrapper.user());
					updateSkin.write(Type.VAR_INT, packetWrapper.user().get(EntityTracker.class).getPlayerId());

					ArrayList<Metadata> metadata = new ArrayList<>();
					metadata.add(new Metadata(10, MetaType1_8.Byte, (byte) flags));

					updateSkin.write(Types1_8.METADATA_LIST, metadata);

					PacketUtil.sendPacket(updateSkin, Protocol1_8TO1_9.class);
				});
			}
		});

		//Client Status

		//Custom Payload
		protocol.registerServerbound(ServerboundPackets1_8.PLUGIN_MESSAGE, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(packetWrapper -> {
					String channel = packetWrapper.get(Type.STRING, 0);
					if (channel.equalsIgnoreCase("MC|BEdit") || channel.equalsIgnoreCase("MC|BSign")) {
						Item book = packetWrapper.passthrough(Type.ITEM);
						book.setIdentifier(386);
						CompoundTag tag = book.tag();
						if (tag.contains("pages")) {
							ListTag pages = tag.get("pages");
							for (int i = 0; i < pages.size(); i++) {
								StringTag page = pages.get(i);
								String value = page.getValue();
								value = ChatUtil.jsonToLegacy(value);
								page.setValue(value);
							}
						}
					} else if (channel.equalsIgnoreCase("MC|AdvCdm")) {
						packetWrapper.set(Type.STRING, 0, channel = "MC|AdvCmd");
					}
				});
			}
		});

		//Spectate
		//Resource Pack Status
	}
}
