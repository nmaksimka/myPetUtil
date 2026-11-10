package io.github.nmaksimka.mypetutil.world;

/**
 * Окно чужой программы.
 *
 * @param handle HWND окна
 * @param rect   границы окна в логических координатах
 * @param exe    имя exe в нижнем регистре, например "chrome.exe"
 * @param title  заголовок окна
 */
public record WindowInfo(long handle, Rect rect, String exe, String title) {
}
