package net.execheinz.upgrader;

import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;

public final class AttemptLimiter {
	private static final Map<UUID, Deque<Long>> ATTEMPTS = new HashMap<UUID, Deque<Long>>();

	private AttemptLimiter() {}

	/** Seconds until the player may try again, 0 if allowed. */
	public static int cooldownRemaining(EntityPlayerMP p) {
		int limit = UpgraderConfig.attemptLimit, window = UpgraderConfig.attemptWindowSeconds;
		if (limit <= 0 || window <= 0) return 0;
		Deque<Long> q = ATTEMPTS.get(p.getUniqueID());
		if (q == null) return 0;
		long w = window * 1000L;
		prune(q, w);
		if (q.isEmpty()) { ATTEMPTS.remove(p.getUniqueID()); return 0; }
		if (q.size() < limit) return 0;
		long free = q.peekFirst() + w;
		return (int) Math.max(1L, (free - System.currentTimeMillis() + 999L) / 1000L);
	}

	public static void record(EntityPlayerMP p) {
		if (UpgraderConfig.attemptLimit <= 0 || UpgraderConfig.attemptWindowSeconds <= 0) return;
		Deque<Long> q = ATTEMPTS.get(p.getUniqueID());
		if (q == null) { q = new ArrayDeque<Long>(); ATTEMPTS.put(p.getUniqueID(), q); }
		prune(q, UpgraderConfig.attemptWindowSeconds * 1000L);
		q.addLast(System.currentTimeMillis());
	}

	public static void forget(UUID id) { ATTEMPTS.remove(id); }

	private static void prune(Deque<Long> q, long windowMs) {
		long cutoff = System.currentTimeMillis() - windowMs;
		while (!q.isEmpty() && q.peekFirst() <= cutoff) q.removeFirst();
	}
}
