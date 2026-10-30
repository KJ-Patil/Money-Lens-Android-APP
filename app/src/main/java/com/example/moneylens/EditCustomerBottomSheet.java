package com.example.moneylens;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.moneylens.databinding.DialogEditCustomerBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditCustomerBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CUSTOMER_ID = "customer_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_PHONE = "phone";

    private DialogEditCustomerBinding binding;
    private List<Country> countries;
    private Country selectedCountry;

    private String customerId;
    private String initialName;
    private String initialPhone;

    public static EditCustomerBottomSheet newInstance(String customerId, String name, String phone) {
        EditCustomerBottomSheet sheet = new EditCustomerBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CUSTOMER_ID, customerId);
        args.putString(ARG_NAME, name);
        args.putString(ARG_PHONE, phone);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getString(ARG_CUSTOMER_ID);
            initialName = getArguments().getString(ARG_NAME);
            initialPhone = getArguments().getString(ARG_PHONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogEditCustomerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        countries = Country.all();
        Country defaultCountry = Country.getByCurrency(CurrencyManager.getInstance().getCurrentCurrencyCode());
        if (defaultCountry == null) defaultCountry = countries.get(0);

        // Prepopulate name
        if (initialName != null) {
            binding.etName.setText(initialName);
        }

        // Prepopulate phone and detect country
        Country.ParsedPhone parsed = Country.parsePhone(initialPhone, defaultCountry);
        selectedCountry = parsed.country;
        updateCountryState();
        if (parsed.nationalNumber != null && !parsed.nationalNumber.isEmpty()) {
            binding.etPhone.setText(parsed.nationalNumber);
            if (binding.etPhone.getText() != null) {
                binding.etPhone.setSelection(binding.etPhone.getText().length());
            }
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnCountryCode.setOnClickListener(v -> showCountryPicker());
        binding.btnSave.setOnClickListener(v -> saveChanges());

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

    private void saveChanges() {
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String rawPhone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            binding.tilName.setError("Please enter contact name");
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

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || customerId == null) {
            Toast.makeText(requireContext(), "Unable to save contact.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", finalPhoneNumber);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("customers").document(customerId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Contact updated successfully", Toast.LENGTH_SHORT).show();
                    if (isAdded()) dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("EditCustomer", "Update failed", e);
                    Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    if (binding != null) {
                        binding.btnSave.setEnabled(true);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
