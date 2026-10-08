-- Test users, do not consider them for production
--   ana: abc123
--   luis: def456
UPDATE users SET password='$2a$15$lxruDSI8BXq39xW2Pe2sNOk5s6mxryqjPg73asOg9dJCmhM3oMj1C' WHERE email = 'ana@taskhub.com';
UPDATE users SET password='$2a$15$1PU5NVTO.i/CSZJ9A/DH9uwMRjPtPzSEGAXWRBCiOTUvvobQ491i2' WHERE email = 'luis@taskhub.com';
