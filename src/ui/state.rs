/// UI State Management for Plus/4 Emulator
/// Handles UI visibility, actions, and file browser state

/// Actions the UI can request from the emulator
#[derive(Debug, Clone)]
pub enum EmulatorAction {
    Reset,
    LoadPrg(String),
    Quit,
}

/// Entry in the PRG file list
#[derive(Debug, Clone)]
pub struct PrgFileEntry {
    pub name: String,
    pub path: String,
    pub size: u64,
}

/// UI visibility and interaction state
pub struct UiState {
    /// Whether the UI overlay is visible
    pub visible: bool,

    /// Whether emulation should be paused when UI is open
    pub pause_emulation: bool,

    /// Selected PRG file path (if any)
    pub selected_prg: Option<String>,

    /// Action requested by UI to be handled by main loop
    pub pending_action: Option<EmulatorAction>,

    /// List of available PRG files
    pub prg_files: Vec<PrgFileEntry>,
}

impl UiState {
    pub fn new() -> Self {
        let mut state = Self {
            visible: false,
            pause_emulation: true,
            selected_prg: None,
            pending_action: None,
            prg_files: Vec::new(),
        };
        state.refresh_prg_list();
        state
    }

    pub fn toggle_visibility(&mut self) {
        self.visible = !self.visible;
        if self.visible {
            self.refresh_prg_list();
        }
    }

    pub fn take_action(&mut self) -> Option<EmulatorAction> {
        self.pending_action.take()
    }

    /// Scan the prg/ directory for PRG files
    pub fn refresh_prg_list(&mut self) {
        self.prg_files.clear();
        if let Ok(entries) = std::fs::read_dir("prg") {
            for entry in entries.flatten() {
                let path = entry.path();
                if let Some(ext) = path.extension() {
                    if ext.to_string_lossy().to_uppercase() == "PRG" {
                        if let Ok(metadata) = entry.metadata() {
                            self.prg_files.push(PrgFileEntry {
                                name: path
                                    .file_name()
                                    .map(|n| n.to_string_lossy().to_string())
                                    .unwrap_or_default(),
                                path: path.to_string_lossy().to_string(),
                                size: metadata.len(),
                            });
                        }
                    }
                }
            }
        }
        // Sort alphabetically
        self.prg_files.sort_by(|a, b| a.name.cmp(&b.name));
    }
}

impl Default for UiState {
    fn default() -> Self {
        Self::new()
    }
}
