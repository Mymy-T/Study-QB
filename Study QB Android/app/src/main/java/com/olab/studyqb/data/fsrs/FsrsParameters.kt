package com.olab.studyqb.data.fsrs

/**
 * FSRS v5 default parameters, per the published open-spaced-repetition FSRS-4.5/5 default
 * weight vector.
 *
 * NOTE: these are transcribed from public FSRS documentation, not read directly from
 * SwiftFSRS's source (which was not available to inspect). [FsrsScheduler] ships with unit
 * tests asserting behavior against hand-computed/reference expectations, but if SwiftFSRS's
 * exact due dates ever need to match bit-for-bit, re-verify this vector and the formulas in
 * [FsrsScheduler] against a reference implementation (e.g. py-fsrs / rs-fsrs).
 */
object FsrsParameters {
    val w: DoubleArray = doubleArrayOf(
        0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0234,
        1.616, 0.1544, 1.0824, 1.9813, 0.0953, 0.2975, 2.2042, 0.2407,
        2.9466, 0.5034, 0.6567
    )

    const val REQUEST_RETENTION = 0.9
    const val MAXIMUM_INTERVAL_DAYS = 36500L
    const val DECAY = -0.5
    val FACTOR = 19.0 / 81.0
}
