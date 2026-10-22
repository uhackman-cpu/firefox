/*
 * User-triggered standalone-window prototype.
 *
 * The extension only acts after the toolbar button is clicked. It does not
 * read page contents, inject scripts, or navigate to a new destination.
 */
browser.action.onClicked.addListener(async tab => {
  if (!tab || !Number.isInteger(tab.id) || !Number.isInteger(tab.windowId)) {
    return;
  }

  let popup;
  try {
    // A popup window presents the selected page with reduced browser chrome.
    popup = await browser.windows.create({
      type: "popup",
      url: "about:blank",
      focused: true
    });

    const blankTab = popup.tabs?.[0];
    await browser.tabs.move(tab.id, {
      windowId: popup.id,
      index: 0
    });

    // Remove the temporary blank tab only after the requested tab has moved.
    if (blankTab && Number.isInteger(blankTab.id) && blankTab.id !== tab.id) {
      await browser.tabs.remove(blankTab.id).catch(() => {});
    }

    await browser.windows.update(popup.id, { focused: true });
  } catch (error) {
    // Avoid silently swallowing failures; surface a short error to the user.
    if (popup?.id) {
      await browser.windows.remove(popup.id).catch(() => {});
    }
    console.error("Could not open standalone tab window:", error);
  }
});
