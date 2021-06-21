package de.gerrygames.viarewind.protocol.protocol1_7_6_10to1_8.types;

import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.type.types.minecraft.MetaListTypeTemplate;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class MetadataListType extends MetaListTypeTemplate {
	private MetadataType metadataType = new MetadataType();

	@Override
	public List<Metadata> read(ByteBuf buffer) throws Exception {
		ArrayList<Metadata> list = new ArrayList<>();

		Metadata m;
		do {
			m = Types1_7_6_10.METADATA.read(buffer);
			if (m != null) {
				list.add(m);
			}
		} while(m != null);

		if (find(2, "Slot", list)!=null && find(8, "Slot", list)!=null) {
			list.removeIf(metadata -> metadata.id()==2 || metadata.id()==3);
		}

		return list;
	}

	private Metadata find(int id, String type, List<Metadata> list) {
		for (Metadata metadata : list) if (metadata.id()==id && metadata.metaType().toString().equals(type)) return metadata;
		return null;
	}

	@Override
	public void write(ByteBuf buffer, List<Metadata> metadata) throws Exception {
		for (Metadata meta : metadata) {
			Types1_7_6_10.METADATA.write(buffer, meta);
		}
		if (metadata.isEmpty()) {
			Types1_7_6_10.METADATA.write(buffer, new Metadata(0, MetaType1_7_6_10.Byte, (byte)0));
		}
		buffer.writeByte(127);
	}
}