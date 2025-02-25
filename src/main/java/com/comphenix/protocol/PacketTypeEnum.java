/*
 *  ProtocolLib - Bukkit server library that allows access to the Minecraft protocol.
 *  Copyright (C) 2012 Kristian S. Stangeland
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307 USA
 */

package com.comphenix.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a more modern object-based enum.
 * <p>
 * This is useful if you want the flexibility of a modern Java enum, but don't
 * want to prevent the creation of additional members dynamically.
 * @author Kristian
 */
public class PacketTypeEnum implements Iterable<PacketType> {
	// Used to convert between IDs and names
	protected Set<PacketType> members = new HashSet<>();

	/**
	 * Registers every declared PacketType field.
	 */
	public PacketTypeEnum() {
		registerAll();
	}

	/**
	 * Registers every public assignable static field as a member.
	 */
	@SuppressWarnings("unchecked")
	protected void registerAll() {
		try {
			// Register every non-deprecated field
			for (Field entry : this.getClass().getFields()) {
				if (Modifier.isStatic(entry.getModifiers()) && PacketType.class.isAssignableFrom(entry.getType())) {
					PacketType packetValue = (PacketType) entry.get(null);
					if (packetValue == null) {
						throw new IllegalArgumentException("Field " + entry.getName() + " was null!");
					}

					packetValue.setName(entry.getName());

					if (entry.getAnnotation(PacketType.ForceAsync.class) != null) {
						packetValue.forceAsync();
					}

					boolean deprecated = entry.getAnnotation(Deprecated.class) != null;
					if (deprecated) packetValue.setDeprecated();

					if (members.contains(packetValue)) {
						// Replace potentially deprecated packet types with non-deprecated ones
						if (!deprecated) {
							members.remove(packetValue);
							members.add(packetValue);
						}
					} else {
						members.add(packetValue);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Registers a member if its not present.
	 * @param instance - member instance.
	 * @param name - name of member.
	 * @return TRUE if the member was registered, FALSE otherwise.
	 */
	public boolean registerMember(PacketType instance, String name) {
		instance.setName(name);

		if (!members.contains(instance)) {
			members.add(instance);
			return true;
		}

		return false;
	}
	
	/**
	 * Determines whether or not the given member has been registered to this enum.
	 * @param member - the member to check.
	 * @return TRUE if the given member has been registered, FALSE otherwise.
	 */
	public boolean hasMember(PacketType member) {
		return members.contains(member);
	}

	/**
	 * Retrieve every registered member.
	 * @return Enumeration of every value.
	 */
	public Set<PacketType> values() {
		return new HashSet<>(members);
	}

	@Override
	public Iterator<PacketType> iterator() {
		return members.iterator();
	}
}
