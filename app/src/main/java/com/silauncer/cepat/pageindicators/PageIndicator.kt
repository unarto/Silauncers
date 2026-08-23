package com.silauncer.cepat.pageindicators

// [Jalur Class]: com.silauncer.cepat.pageindicators.PageIndicator
// [Penjelasan]: Interface kontrak dasar untuk komponen penunjuk halaman (Page Indicator) di launcher yang mengontrol posisi scroll, indeks halaman aktif, jumlah marker halaman, serta kontrol transisi animasi.
interface PageIndicator {

    /**
     * Memperbarui status scroll saat ini terhadap total scroll keseluruhan halaman.
     */
    fun setScroll(currentScroll: Int, totalScroll: Int)

    /**
     * Menetapkan penanda (marker) aktif sesuai indeks halaman.
     */
    fun setActiveMarker(activePage: Int)

    /**
     * Menetapkan total jumlah penanda/halaman yang tersedia.
     */
    fun setMarkersCount(numMarkers: Int)

    /**
     * Mengatur apakah indikator halaman harus menghilang otomatis (auto-hide) setelah jeda diam.
     */
    fun setShouldAutoHide(shouldAutoHide: Boolean) {}

    /**
     * Menjeda (pause) semua animasi indikator yang sedang berjalan.
     */
    fun pauseAnimations() {}

    /**
     * Memaksa animasi langsung selesai ke kondisi akhir.
     */
    fun skipAnimationsToEnd() {}

    /**
     * Mengatur warna kuas cat penggambaran indikator.
     */
    fun setPaintColor(color: Int) {}
}
