/**
 * The mod's <b>outbound</b> integration points on NeoForge: how another mod hears about, and can
 * refuse, things that happen in Hempdustry. Four events, posted on
 * {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS}, the twins of Fabric's
 * {@code HempdustryEvents} and carrying the same payloads: {@link AllowSmokeEvent} (cancel it to
 * refuse a hit), {@link AfterSmokeEvent}, {@link AfterEatEvent} and {@link AfterInfuseEvent}.
 *
 * <p><b>Consumption has two doors</b>, and anything counting it needs both: {@link AfterSmokeEvent}
 * for a hit and {@link AfterEatEvent} for an edible.
 *
 * <h2>What is promised</h2>
 * <b>This package will not break</b> on NeoForge: from the first public NeoForge build (2.1.0-beta)
 * it is additive-only -- an event or a getter may be added, never removed or re-shaped. The same
 * promise covers the types these events carry ({@code SmokeContents}, {@code Quality} and
 * {@code com.warlonmhite.hempdustry.strain.Strain}). A Fabric jar and a NeoForge jar are separate
 * builds, and an addon is compiled against the one it runs with.
 *
 * <h2>Threading and sides</h2>
 * Every event here is posted <b>on the server thread only</b>. A listener may read and change world
 * state directly; it must not block.
 */
package com.warlonmhite.hempdustry.api;
