package com.example.moneylens;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.moneylens.databinding.DialogAddAccountBookBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class AddAccountBookBottomSheet extends BottomSheetDialogFragment {

    private DialogAddAccountBookBinding binding;
    /** Direction of the opening balance only — "they owe you" is the common case. */
    private boolean theyOweUs = true;

    private List<Country> countries;
    private Country selectedCountry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogAddAccountBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show currency code in balance label
        String currencyCode = CurrencyManager.getInstance().getCurrentCurrencyCode();
        binding.txtBalanceLabel.setText("OPENING BALANCE (" + currencyCode + ")");

        setupToggle();
        setupCountrySelector();
        setupListeners();
    }

    private void setupCountrySelector() {
        countries = Country.all();
        Country defaultCountry = Country.getByCurrency(CurrencyManager.getInstance().getCurrentCurrencyCode());
        selectedCountry = defaultCountry != null ? defaultCountry : countries.get(0);
        updateCountryState();

        binding.btnCountryCode.setOnClickListener(v -> showCountryPicker());

        binding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) return;
                String text = s.toString();

                if (text.startsWith("+")) {
                    Country.ParsedPhone parsed = Country.parsePhone(text, selectedCountry);
                    selectedCountry = parsed.country;
                    updateCountryState();
                    binding.etPhone.removeTextChangedListener(this);
                    binding.etPhone.setText(parsed.nationalNumber);
                    if (binding.etPhone.getText() != null) {
                        binding.etPhone.setSelection(binding.etPhone.getText().length());
                    }
                    binding.etPhone.addTextChangedListener(this);
                    return;
                }

                // If user entered a leading '0' (domestic trunk prefix in India e.g. 09876543210 -> 9876543210)
                if (text.startsWith("0") && text.length() > 1 && "IN".equals(selectedCountry.isoCode)) {
                    String trimmed = text.substring(1);
                    binding.etPhone.removeTextChangedListener(this);
                    binding.etPhone.setText(trimmed);
                    binding.etPhone.setSelection(trimmed.length());
                    binding.etPhone.addTextChangedListener(this);
                    return;
                }

                if (binding.tilPhone.getError() != null) {
                    String digits = Country.digitsOnly(text);
                    if (digits.length() >= selectedCountry.minDigits && digits.length() <= selectedCountry.maxDigits) {
                        binding.tilPhone.setError(null);
                    }
                }
            }
        });
    }

    private void updateCountryState() {
        if (binding == null || selectedCountry == null) return;
        binding.btnCountryCode.setText(selectedCountry.flag() + " " + selectedCountry.dialCode);

        if (selectedCountry.minDigits == selectedCountry.maxDigits) {
            binding.tilPhone.setHint(selectedCountry.maxDigits + "-digit mobile number");
        } else {
            binding.tilPhone.setHint(selectedCountry.minDigits + "–" + selectedCountry.maxDigits + " digits");
        }

        binding.etPhone.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(selectedCountry.maxDigits)
        });

        String current = binding.etPhone.getText() != null ? binding.etPhone.getText().toString() : "";
        String digits = Country.digitsOnly(current);
        if (digits.length() > selectedCountry.maxDigits) {
            digits = digits.substring(0, selectedCountry.maxDigits);
            binding.etPhone.setText(digits);
            binding.etPhone.setSelection(digits.length());
        }
    }

    private void showCountryPicker() {
        if (getContext() == null) return;
        String[] labels = new String[countries.size()];
        for (int i = 0; i < countries.size(); i++) {
            labels[i] = countries.get(i).displayLabel();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Country")
                .setItems(labels, (dialog, which) -> {
                    selectedCountry = countries.get(which);
                    updateCountryState();
                })
                .show();
    }

    private void setupToggle() {
        applyToggleState(true);

        binding.toggleOpeningDirection.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            theyOweUs = checkedId == R.id.btnOpeningTheyOwe;
            applyToggleState(theyOweUs);
        });
    }

    private void applyToggleState(boolean theyOwe) {
        if (theyOwe) {
            binding.btnOpeningTheyOwe.setBackgroundColor(Color.parseColor("#7C6FE0"));
            binding.btnOpeningTheyOwe.setTextColor(Color.WHITE);
            binding.btnOpeningYouOwe.setBackgroundColor(Color.TRANSPARENT);
            binding.btnOpeningYouOwe.setTextColor(Color.parseColor("#7C6FE0"));
        } else {
            binding.btnOpeningYouOwe.setBackgroundColor(Color.parseColor("#7C6FE0"));
            binding.btnOpeningYouOwe.setTextColor(Color.WHITE);
            binding.btnOpeningTheyOwe.setBackgroundColor(Color.TRANSPARENT);
            binding.btnOpeningTheyOwe.setTextColor(Color.parseColor("#7C6FE0"));
        }
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnImportContact.setOnClickListener(v -> pickContact());
        binding.btnCreate.setOnClickListener(v -> createBook());
    }

    private void pickContact() {
        ContactPickerBottomSheet picker = ContactPickerBottomSheet.newInstance(false);
        picker.setListener(contacts -> {
            if (contacts.isEmpty() || binding == null) return;
            ContactPickerBottomSheet.Contact c = contacts.get(0);
            binding.etName.setText(c.name);

            Country.ParsedPhone parsed = Country.parsePhone(c.phone, selectedCountry);
            selectedCountry = parsed.country;
            updateCountryState();
            binding.etPhone.setText(parsed.nationalNumber);
            if (binding.etPhone.getText() != null) {
                binding.etPhone.setSelection(binding.etPhone.getText().length());
            }
        });
        picker.show(getChildFragmentManager(), "contact_picker");
    }

    private void createBook() {
        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String rawPhone = binding.etPhone.getText() != null
                ? binding.etPhone.getText().toString().trim() : "";
        String balanceStr = binding.etOpeningBalance.getText() != null
                ? binding.etOpeningBalance.getText().toString().trim() : "0";

        if (name.isEmpty()) {
            binding.tilName.setError("Please enter account name");
            return;
        }
        binding.tilName.setError(null);

        String nationalDigits = Country.digitsOnly(rawPhone);
        String finalPhoneNumber = "";
        if (!nationalDigits.isEmpty()) {
            String phoneError = selectedCountry.validatePhone(nationalDigits);
            if (phoneError != null) {
                binding.tilPhone.setError(phoneError);
                return;
            }
            binding.tilPhone.setError(null);
            finalPhoneNumber = selectedCountry.dialCode + " " + nationalDigits;
        } else {
            binding.tilPhone.setError(null);
        }

        double openingBalance = 0.0;
        try {
            openingBalance = Double.parseDouble(balanceStr.isEmpty() ? "0" : balanceStr);
        } catch (NumberFormatException ignored) {}

        // The toggle only decides the sign of this opening figure. It is typed in the
        // active currency and stored, like every amount, in the base currency.
        openingBalance = CurrencyManager.getInstance().toBaseAmount(
                theyOweUs ? Math.abs(openingBalance) : -Math.abs(openingBalance));

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Session expired. Please sign in again.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.btnCreate.setEnabled(false);
        String uid = auth.getCurrentUser().getUid();

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("customers").document();
        String customerId = docRef.getId();

        Customer customer = new Customer(customerId, name, finalPhoneNumber,
                openingBalance, Timestamp.now());

        docRef.set(customer.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddAccountBook", "Firestore write failed", e);
                    // Toast is always visible — unlike Snackbar it doesn't need a CoordinatorLayout
                    Toast.makeText(requireContext(),
                            "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Re-enable button only if the view is still alive
                    if (binding != null) {
                        binding.btnCreate.setEnabled(true);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
