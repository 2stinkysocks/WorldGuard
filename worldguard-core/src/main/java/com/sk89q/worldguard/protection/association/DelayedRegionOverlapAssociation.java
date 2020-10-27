/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.protection.association;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Determines that the association to a region is {@code OWNER} if the input
 * region is in a set of source regions.
 *
 * <p>This class only performs a spatial query if its
 * {@link #getAssociation(List)} method is called.</p>
 */
public class DelayedRegionOverlapAssociation extends AbstractRegionOverlapAssociation {

    private final RegionQuery query;
    private final Location location;

    /**
     * Create a new instance.
     * @param query the query
     * @param location the location
     */
    public DelayedRegionOverlapAssociation(RegionQuery query, Location location) {
        this(query, location, false);
    }

    /**
     * Create a new instance.
     * @param query the query
     * @param location the location
     * @param useMaxPriorityAssociation whether to use the max priority from regions to determine association
     */
    public DelayedRegionOverlapAssociation(RegionQuery query, Location location, boolean useMaxPriorityAssociation) {
        super(null, useMaxPriorityAssociation);
        checkNotNull(query);
        checkNotNull(location);
        this.query = query;
        this.location = location;
    }

    @Override
    public Association getAssociation(List<ProtectedRegion> regions) {
        if (source == null) {
            ApplicableRegionSet result = query.getApplicableRegions(location);

            if (result.queryAllValues(null, Flags.PROTECT_SURROUNDINGS).contains(true)) {
                Set<ProtectedRegion> source = new HashSet<>();
                Collection<ProtectedRegion> all = WorldGuard.getInstance().getPlatform().getRegionContainer()
                        .get((World) location.getExtent()).getRegions().values();

                for (ProtectedRegion resultRegion : result) {
                    if (Boolean.TRUE.equals(resultRegion.getFlag(Flags.PROTECT_SURROUNDINGS))) {
                        source.add(resultRegion);

                        for (UUID uniqueId : resultRegion.getOwners().getUniqueIds()) {
                            for (ProtectedRegion region : all) {
                                if (region.getOwners().contains(uniqueId)) {
                                    source.add(region);
                                }
                            }
                        }

                        for (String player : resultRegion.getOwners().getPlayers()) {
                            for (ProtectedRegion region : all) {
                                if (region.getOwners().contains(player)) {
                                    source.add(region);
                                }
                            }
                        }
                    }
                }

                this.source = source;
            } else {
                source = result.getRegions();
            }

            calcMaxPriority();
        }

        return super.getAssociation(regions);
    }

}
