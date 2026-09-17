/**
 * Copyright (c) 2019 Gregorius Techneticies
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.log;
import gregapi.util.WD;

import java.io.PrintStream;
import java.util.ArrayList;

import net.neoforged.bus.api.SubscribeEvent;
import gregapi.util.UT;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * @author Gregorius Techneticies
 */
public class LoggerPlayerActivity implements Runnable {
	private ArrayList<String> mBufferedPlayerActivity = new ArrayList<>();

	/** The journal thread remembers itself inside run(), so the start point stays the same call as the original,
	 *  and shutdown still knows which thread to wake. */
	private volatile Thread mThread = null;

	public static PrintStream mLog = null;
	
	public LoggerPlayerActivity(PrintStream aLog) {
		NeoForge.EVENT_BUS.register(this);
		mLog = aLog;
	}
	
	// The single 1.7.10 PlayerInteractEvent is split into subclasses in neo, and the abstract base rejects
	// subscriptions, so the handler is split per concrete sub-event while matching the same click filter.
	@SubscribeEvent
	public void onPlayerInteractionLeftClickBlock(PlayerInteractEvent.LeftClickBlock aEvent) {logInteraction(aEvent, "LEFT_CLICK_BLOCK");}

	@SubscribeEvent
	public void onPlayerInteractionRightClickBlock(PlayerInteractEvent.RightClickBlock aEvent) {logInteraction(aEvent, "RIGHT_CLICK_BLOCK");}

	/** Body of the former single handler, shared so the logging line stays in one place for both sub-events. */
	private void logInteraction(PlayerInteractEvent aEvent, String aAction) {
		if (aEvent.getEntity() != null && aEvent.getLevel() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";"+aAction+";"+aEvent.getEntity().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	// F7-event: 1.7.10 BlockEvent.HarvestDropsEvent -> neo BlockDropsEvent (event/level). harvester->getBreaker():Entity,
	// world->getLevel():ServerLevel, x/y/z->getPos().
	@SubscribeEvent
	public void onBlockHarvestingEvent(net.neoforged.neoforge.event.level.BlockDropsEvent aEvent) {
		if (aEvent.getBreaker() != null && !aEvent.getLevel().isClientSide() && mLog != null) mBufferedPlayerActivity.add(UT.Code.dateAndTime()+";HARVEST_BLOCK;"+aEvent.getBreaker().getName().getString()+";DIM:"+WD.dimensionId(aEvent.getLevel())+";"+aEvent.getPos().getX()+";"+aEvent.getPos().getY()+";"+aEvent.getPos().getZ()+";|;"+aEvent.getPos().getX()/10+";"+aEvent.getPos().getY()/10+";"+aEvent.getPos().getZ()/10);
	}
	
	@Override
	public void run() {
		mThread = Thread.currentThread();
		while (true) {try {
			if (mLog == null) return;
			flush(mLog);
			Thread.sleep(10000);
		} catch(Throwable e) {/**/}}
	}

	/** Flushing is pulled out of the loop body because it is needed twice, on each tick and on shutdown; there
	 *  should not be a second copy of this logic. */
	private void flush(PrintStream aLog) {
		ArrayList<String> tList = mBufferedPlayerActivity;
		mBufferedPlayerActivity = new ArrayList<>();
		String tLastOutput = "";
		for (int i = 0, j = tList.size(); i < j; i++) {
			if (!tLastOutput.equals(tList.get(i))) aLog.println(tList.get(i));
			tLastOutput = tList.get(i);
		}
	}

	/** Called from the mod's own shutdown hook alongside everything else. Order matters: silence the log flag,
	 *  wake the sleeping thread with an interrupt, wait for its own exit check, then flush and close the file. */
	public void stop() {
		PrintStream tLog = mLog;
		if (tLog == null) return;
		mLog = null;
		Thread tThread = mThread;
		if (tThread != null) {
			tThread.interrupt();
			try {tThread.join(5000);} catch(InterruptedException e) {Thread.currentThread().interrupt();}
		}
		flush(tLog);
		tLog.close();
	}
}
