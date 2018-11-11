/*
 * AsyncWorldEdit API
 * Copyright (c) 2015, SBPrime <https://github.com/SBPrime/>
 * Copyright (c) AsyncWorldEdit API contributors
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted free of charge provided that the following 
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution,
 * 3. Redistributions of source code, with or without modification, in any form 
 *    other then free of charge is not allowed,
 * 4. Redistributions in binary form in any form other then free of charge is 
 *    not allowed.
 * 5. Any derived work based on or containing parts of this software must reproduce 
 *    the above copyright notice, this list of conditions and the following 
 *    disclaimer in the documentation and/or other materials provided with the 
 *    derived work.
 * 6. The original author of the software is allowed to change the license 
 *    terms or the entire license of the software as he sees fit.
 * 7. The original author of the software is allowed to sublicense the software 
 *    or its parts using any license terms he sees fit.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.primesoft.asyncworldedit.api.worldedit;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.Iterator;
import java.util.Map;
import org.primesoft.asyncworldedit.api.IWorld;
import org.primesoft.asyncworldedit.api.blockPlacer.IBlockPlacer;
import org.primesoft.asyncworldedit.api.blockPlacer.entries.IJobEntry;
import org.primesoft.asyncworldedit.api.playerManager.IPlayerEntry;

/**
 *
 * @author SBPprime
 */
public interface IThreadSafeEditSession extends IAweEditSession {

    /**
     * Add async job
     *
     * @param job
     */
    void addAsync(IJobEntry job);

    /**
     * This function checks if async mode is enabled for specific command
     *
     * @param operationName
     * @return
     */
    boolean checkAsync(String operationName);


    IBlockPlacer getBlockPlacer();

    IWorld getCBWorld();

    EditSessionEvent getEditSessionEvent();

    EventBus getEventBus();

    Object getMutex();

    IPlayerEntry getPlayer();
    
    /**
     * Get the root changeset entry
     * @return 
     */
    ChangeSet getRootChangeSet();

    /**
     * Check if async mode is forced
     *
     * @return
     */
    boolean isAsyncForced();

    /**
     * Remov async job (done or canceled)
     *
     * @param job
     */
    void removeAsync(IJobEntry job);

    /**
     * Reset async disabled inner state (enable async mode)
     */
    void resetAsync();

    /**
     * Enables or disables the async mode configuration bypass this function
     * should by used only by other plugins
     *
     * @param value true to enable async mode force
     */
    void setAsyncForced(boolean value);
    
    /**
     * Enables or disables the async mode configuration bypass.
     * This function should by used only by other plugins.
     *
     * @param value true to enable asunc mpde disable forced
     */
    void setAsyncForcedDisable(boolean value);

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @param stage the level
     * @return whether the block changed
     * @throws WorldEditException thrown on a set error
     */
    boolean setBlock(int jobId, BlockVector3 position, BlockStateHolder block, Stage stage) throws WorldEditException;

    boolean setBlock(BlockVector3 vector, BlockStateHolder bsh, int jobId) throws WorldEditException;
    
    boolean setBlock(BlockVector3 position, Pattern pattern, int jobId) throws MaxChangedBlocksException;
    
    Iterator<Change> doUndo();

    Iterator<Change> doRedo();
}