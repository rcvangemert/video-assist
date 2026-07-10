# **Video Assist Production Manual**

**App Version:** v1.2.21 | **Document Version:** 2.0.0 | **Date:** 2026-03-02  
**Documentation Support:** Gemini
---

## **1\. Introduction**

**Video Assist** provides real-time production tools for Android. The platform supports:

* **Cameras:** Seamless switching between front, back, and external camera sources.
* **Audio:** Integration for internal and external microphones.
* **Media:** Dual media players for clips and live sources.
* **Graphics:** A Character generator for PDF, JPG, PNG, and custom templates.
* **Output:** Integrated video/audio mixing with simultaneous recording and streaming.
* **Orientation:** Supports 16:9 Landscape and 9:16 Portrait.

## **2\. User Interface & Layouts**

### **2.1 Tablet Mode (Full Studio View)**

* **The Gallery:** Five dedicated miniatures: **CAM**, **MP1**, **MP2**, **CG**, and **PGM**.
* **The Command Center:** Three-column workflow consisting of **Show Items** (Left), **Markup Script** (Center), and **Event List** (Right).

### **2.2 Phone Mode (Compact View with Switcher)**

* **Dual Monitor Stack:** **Program** (Top) and a switchable **Preview** (Bottom).
* **Toolbar Preview Switcher:** Switch to and monitor each source individually.
* **The Command Center:** Three-column workflow consisting of **Show Items** (Left), **Monitoring** (Center), and **Event List** (Right).
* **Markup script editor:** Click the toolbar pen icon for a full screen editor.

### **2.3 Quick Reference Tables**

#### **2.3.1 Camera Controls**

| Position | Function | Description |
| :---- | :---- | :---- |
| **Top Left** | Front Camera | Select user-facing camera. |
| **Mid Left** | Back/External (usb) | Switch to rear camera or USB/External source. |
| **Bottom Center Left** | Take Options | Configure Video/Audio transition modes. |
| **Bottom Center Right** | Camera options | Controls zoom, auto exposure lock and auto white balance lock. |
| **Center (T)** | **TAKE** | Send source directly to Program (PGM). |
| **Top Right** | Pre-listen | Listen to source (microphone) without switching to program. |
| **Right Fader** | Mic Level | Adjust input gain (0 to 120), 100 is nominal. |

#### **2.3.2 Media Player (MP1 & MP2)**

| Position | Function | Description |
| :---- | :---- | :---- |
| **Top Left** | Load Source | Select media; displays thumbnail and duration. |
| **Mid Left** | Play | Play clip with time display. |
| **Center (T)** | **TAKE** | Send clip directly to Program (PGM). |
| **Bottom Center Left** | Take Options | Configure Video/Audio transition modes. |
| **Bottom Center Right** | Media player options | Enable loop. |
| **Top Right** | Pre-listen | Listen to source without switching to program. |
| **Right Fader** | Audio Level | Adjust replay level. |

#### **2.3.3 Character Generator (CG)**

| Position | Function | Description |
| :---- | :---- | :---- |
| **Top Left** | Load Source | Select PDF, JPG, PNG, or Template files. |
| **Mid Left** | Key Up/Dn | Manually superimpose (DSK) CG over Program. |
| **Center (T)** | **TAKE** | Send graphic directly to Program (PGM). |
| **Bottom Center Left** | Take Options | Configure Video transition modes. |

### 

### **⚠️ OPERATIONAL WARNING: The Keyer Trap**

The **Key Up/Dn** button glows **RED** when active. Because the Keyer is the top-most layer, it can hide all other sources. Ensure scripts end with `*cg key off`.

#### **2.3.4 Program (PGM) Controls**

| Position | Function | Description |
| :---- | :---- | :---- |
| **Mid Left** | Pre-listen | Listen to the microphone of a receiving remote source. |
| **Top Right** | Record | Master toggle to start/stop PGM recording. |
| **Mid Right** | Stream | Master toggle to start/stop live stream output. |

## **3\. Production Workflow**

The app is modeled on a **TV News Production workflow**:

1. **The Show:** The highest level of organization.
2. **The Item:** Individual segments within a show.
3. **The Event:** Specific commands (camera cuts, graphics, clips) within an item.

### **3.1 Show and Item Management**

#### **3.1.1 The Show List**

To access shows, tap **Home** or **swipe left-to-right**.

* **Show Options:** Tap the **Options (⋮)** icon to Create New, Delete, Rename, or Export/Import shows to `Documents/VideoAssist/shows`.
* **Reordering:** **Long-click** a show to move its position in the list.

#### **3.1.2 The Item List**

* **Item Options:** Tap the **Options (⋮)** icon to Create New, Delete, or Rename segments.
* **Reordering:** **Long-click** an item to move its position in the list.

## **4\. Automation & Scripting**

Video Assist includes a **Script Editor** using a simplified production command language.

* **The TAKE Button:** Executes scripted commands sequentially.
* **Trigger Times:** Precision timing with **one-second accuracy** for scripted events.
* **Manual Override:** Manually select sources by tapping the **Take Icon (T)**.

### **4.1 Scripting Syntax**

* **Camera:** `*cam {front|back} {zoom nn} {ael} {awbl} {video cut|mix nn|none} {audio follow|over|none} {at m:s} {level nn}`
* **Media Player:** `*mp1 {filename} {from m:s} {video cut|mix nn|none} {audio follow|over|none}{loop} {at m:s} {level nn}`
* **CG:** `*cg {filename} {at m:s} {page nn} {key on|off}`
* **TTS:** `*tts {text content}{at m:s}`
* **Recorder:** `*rec {filename} {state} {at m:s}`

### 

### **4.2 Audio Logic**

* **follow**: Fades current sources down and the new source up.
* **over**: Adds the new source on top of current active audio.
* **none**: The source is introduced silently (ideal for B-roll).
* **Timing:** All audio changes feature a **500 mSec fade duration**.

### 

### **4.3 Script to Event Conversion (The Rundown)**

| Markup Script (Command) | Resulting Rundown Events |
| :---- | :---- |
| `*cg video_assist page 1` | **1 Open cg**: `video_assist`, `page 1` |
|  | **2 Take cg** |
| `*rec on at 0:01` | **3 Start record** |
| `*cg box1 key on` | **4 Open cg**: `box1` |
|  | **5 Key on** (Button on CG miniature turns **RED**) |

## **5\. Graphic Template Editor**

Access the **Graphic Activity** via the main toolbar to create and edit **XML** templates. Templates define the layout, fills (Camera, Image, or Text), and alignment for your broadcast overlays.

### **5.1 Template Examples**

| Template | Description | Use Case |
| :---- | :---- | :---- |
| **box1** | Single box \+ label | Solo presenter or full-screen media. |
| **box2** | Two-way split \+ labels | Interviews between local cam and remote guest. |

## **6\. Live Streaming Operations**

To start a stream, place a source on PGM and tap the **Stream** button (middle right of PGM).

### **6.1 The Juice Protocol (Remote Guest)**

* **Logic:** Exclusive connection logic—if a second device attempts to connect to a receiving UUID, the first device will be automatically disconnected.
* **Monitoring:** Use **Connected Device Pre-listen** (PGM middle-left) for guest talkback.
* **YouTube/RTMP:** Standard setups requiring Google Authentication or Server URL/Keys.

## **7\. Recording & Program Snapshots**

### **7.1 Video Recording**

Configure resolution (360p-1080p) and bitrate (500k-4000k) in Settings.

* **Naming:** Uses the "Clip Filename Tag"  in settings with auto-incrementing (e.g., `movie.1.png`).

### **7.2 Program Snapshots**

Tap the **Camera Icon** (PGM bottom right) to capture a `.png` of the live output.

* **Naming:** Uses the "Picture Filename Tag" in settings with auto-incrementing (e.g., `picture.1.png`).

## **8\. Remote Identifiers: URI Shortcuts**

Use **URI Shortcuts** in Settings to map friendly names to complex addresses.

* **Juice:** `robert` \-\> `juice://28e39900...`
* **Self:** A default loopback shortcut pointing to the current device's own UUID.

## **9\. File Management: Internal & External**

### **9.1 Storage Safety**

* **Private Storage:** All data is stored in the app sandbox. **CRITICAL: Uninstalling the app DELETES all internal media.**
* **Import:** Use the Android **Share** menu from any external app and select Video Assist.
* **Export:** Use the **Copy to Document** option in the File Dialog to move files to the public `Documents/VideoAssist` folder.

## 

## 
