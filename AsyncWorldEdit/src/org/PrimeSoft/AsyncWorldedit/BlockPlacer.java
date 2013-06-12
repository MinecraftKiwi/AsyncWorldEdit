/*
 * The MIT License
 *
 * Copyright 2013 SBPrime.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.primesoft.asyncworldedit;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.util.*;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.primesoft.asyncworldedit.blocklogger.IBlockLogger;
import org.primesoft.asyncworldedit.blocklogger.NoneLogger;
import org.primesoft.asyncworldedit.worldedit.AsyncEditSession;

/**
 *
 * @author SBPrime
 */
public class BlockPlacer implements Runnable {
    /**
     * Bukkit scheduler
     */
    private BukkitScheduler m_scheduler;
    /**
     * Current scheduler task
     */
    private BukkitTask m_task;
    /**
     * Logged events queue (per player)
     */
    private HashMap<String, Queue<BlockPlacerEntry>> m_blocks;
    
    
    /**
     * All locked queues
     */
    private HashSet<String> m_lockedQueues;
    
    
    /**
     * Should block places shut down
     */
    private boolean m_shutdown;
    /**
     * The block logger
     */
    private IBlockLogger m_logger;
    
    private int m_queueHardLimit;
    private int m_queueSoftLimit;

    /**
     * Initialize new instance of the block placer
     *
     * @param plugin parent
     * @param blockLogger instance block logger
     */
    public BlockPlacer(PluginMain plugin) {
        m_blocks = new HashMap<String, Queue<BlockPlacerEntry>>();
        m_lockedQueues = new HashSet<String>();
        m_scheduler = plugin.getServer().getScheduler();
        m_task = m_scheduler.runTaskTimer(plugin, this,
                ConfigProvider.getInterval(), ConfigProvider.getInterval());
        
        m_queueHardLimit = ConfigProvider.getQueueHardLimit();
        m_queueSoftLimit = ConfigProvider.getQueueSoftLimit();
        
        setLogger(null);
    }

    
    /**
     * Set the logger
     * @param logger 
     */
    public void setLogger(IBlockLogger logger) {
        if (logger == null)
        {
            logger = new NoneLogger();
        }
        m_logger = logger;
    }
    
    
    /**
     * Block placer main loop
     */
    @Override
    public void run() {
        List<BlockPlacerEntry> entries = 
                new ArrayList<BlockPlacerEntry>(ConfigProvider.getBlockCount());
        synchronized (this) {
            String[] keys = m_blocks.keySet().toArray(new String[0]);
            int keyPos = 0;
            boolean added = keys.length > 0;
            final int blockCnt = ConfigProvider.getBlockCount();
            for (int i = 0; i < blockCnt && added; i++) {
                added = false;

                String player = keys[keyPos];
                Queue<BlockPlacerEntry> queue = m_blocks.get(player);
                if (queue != null) {
                    if (!queue.isEmpty()) {
                        entries.add(queue.poll());
                        added = true;
                    }
                    int size = queue.size();
                    if (size < m_queueSoftLimit && m_lockedQueues.contains(player))
                    {
                        PluginMain.Say(PluginMain.getPlayer(player), "Your block queue is unlocked. You can use WorldEdit.");
                        m_lockedQueues.remove(player);
                    }
                    if (size == 0) {
                        m_blocks.remove(keys[keyPos]);
                    }                                        
                } else  if (m_lockedQueues.contains(player)) {
                    PluginMain.Say(PluginMain.getPlayer(player), "Your block queue is unlocked. You can use WorldEdit.");
                    m_lockedQueues.remove(player);
                }
                keyPos = (keyPos + 1) % keys.length;
            }

            if (!added && m_shutdown) {
                stop();
            }
        }

        for (BlockPlacerEntry entry : entries) {
            process(entry);
        }
    }

    /**
     * Queue stop command
     */
    public void queueStop() {
        m_shutdown = true;
    }

    /**
     * stop block logger
     */
    public void stop() {
        m_task.cancel();
    }

    /**
     * Add task to perform in async mode
     *
     */
    public boolean addTasks(BlockPlacerEntry entry) {
        synchronized (this) {
            AsyncEditSession editSesson = entry.getEditSession();            
            String player = editSesson.getPlayer();
            Queue<BlockPlacerEntry> queue;
            if (!m_blocks.containsKey(player)) {
                queue = new ArrayDeque<BlockPlacerEntry>();
                m_blocks.put(player, queue);
            } else {
                queue = m_blocks.get(player);
            }
            
            if (m_lockedQueues.contains(player))
            {
                return false;
            }            
            
            queue.add(entry);
            if (queue.size() >= m_queueHardLimit && 
                !PermissionManager.isAllowed(PluginMain.getPlayer(player), PermissionManager.Perms.QueueBypass))
            {
                m_lockedQueues.add(player);
                PluginMain.Say(PluginMain.getPlayer(player), "Your block queue is full. Wait for items to finish drawing.");
                return false;
            }

            return true;
        }
    }

    
    /**
     * Remove all entries for player
     *
     * @param player
     */
    public void purge(String player) {
        synchronized (this) {
            if (m_blocks.containsKey(player)) {
                m_blocks.remove(player);
            }
            if (m_lockedQueues.contains(player))
            {
                m_lockedQueues.remove(player);
            }
        }
    }
    
    
    /**
     * Remove all entries 
     */
    public void purgeAll() {
        synchronized (this) {
            for (String user : getAllPlayers())
            {
                purge(user);
            }
        }
    }
    
    
    /**
     * Get all players in log
     *
     * @return players list
     */
    public String[] getAllPlayers() {
        synchronized (this) {
            return m_blocks.keySet().toArray(new String[0]);
        }
    }

    
    /**
     * Gets the number of events for a player
     *
     * @param player player login
     * @return number of stored events
     */
    public int getPlayerEvents(String player) {
        synchronized (this) {
            if (m_blocks.containsKey(player)) {
                return m_blocks.get(player).size();
            }

            return 0;
        }
    }

    /**
     * Process logged event
     *
     * @param entry event to process
     */
    private void process(BlockPlacerEntry entry) {
        if (entry == null) {
            return;
        }

        Vector location = entry.getLocation();
        BaseBlock block = entry.getNewBlock();        
        AsyncEditSession eSession = entry.getEditSession();
        String player = eSession.getPlayer();
        World world = eSession.getCBWorld();
        
        BaseBlock oldBlock = eSession.getBlock(location);
        boolean success = eSession.doRawSetBlock(location, block);
        
        if(success && world != null) {
        	m_logger.LogBlock(location, oldBlock, block, player, world);
        }
    }    
}