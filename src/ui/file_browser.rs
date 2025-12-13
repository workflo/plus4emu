/// PRG File Browser Component for Plus/4 Emulator

use egui_macroquad::egui::{self, ScrollArea, Ui};

use super::state::{EmulatorAction, UiState};

/// Render the PRG file browser panel
pub fn render_file_browser(ui: &mut Ui, state: &mut UiState) {
    ui.horizontal(|ui| {
        ui.heading("PRG Files");
        if ui.button("Refresh").clicked() {
            state.refresh_prg_list();
        }
    });

    ui.separator();

    if state.prg_files.is_empty() {
        ui.label("No PRG files found in prg/ directory");
    } else {
        ui.label(format!("{} files found", state.prg_files.len()));
        ui.add_space(4.0);

        ScrollArea::vertical()
            .max_height(300.0)
            .show(ui, |ui| {
                // Clone the list to avoid borrow issues
                let files: Vec<_> = state.prg_files.iter().cloned().collect();

                for entry in &files {
                    let is_selected = state.selected_prg.as_ref() == Some(&entry.path);

                    let label = format!("{} ({} bytes)", entry.name, entry.size);
                    let response = ui.selectable_label(is_selected, &label);

                    if response.clicked() {
                        state.selected_prg = Some(entry.path.clone());
                    }

                    if response.double_clicked() {
                        state.pending_action =
                            Some(EmulatorAction::LoadPrg(entry.path.clone()));
                        state.visible = false;
                    }
                }
            });

        ui.add_space(8.0);

        ui.horizontal(|ui| {
            let load_enabled = state.selected_prg.is_some();
            if ui
                .add_enabled(load_enabled, egui::Button::new("Load Selected"))
                .clicked()
            {
                if let Some(path) = state.selected_prg.clone() {
                    state.pending_action = Some(EmulatorAction::LoadPrg(path));
                    state.visible = false;
                }
            }
        });
    }
}
