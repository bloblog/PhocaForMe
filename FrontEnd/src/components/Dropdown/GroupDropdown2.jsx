import * as React from "react";
import { useState, useEffect } from "react";

import { Box, TextField, Autocomplete } from "@mui/material";
import { getIdolGroup } from "@/api/idolinfo";

const GroupDropdown2 = ({ defaultGroup, isProfile, onChange }) => {
  const [groupItems, setGroupItems] = useState([]);

  useEffect(() => {
    getIdolGroup(
      (data) => {
        setGroupItems(data.data);
      },
      (error) => console.error("그룹 세팅 오류:", error)
    );
  }, []);

  const [value, setValue] = useState(isProfile ? null : defaultGroup);

  const handleChange = (event, newValue) => {
    setValue(newValue);
    onChange(newValue);
  };

  return (
    <Autocomplete
      value={value}
      onChange={handleChange}
      size="small"
      id="group-dropdown"
      options={groupItems}
      getOptionLabel={(option) =>
        `${option.idolGroupNameKr} (${option.idolGroupNameEng})`
      }
      isOptionEqualToValue={(option, value) =>
        option.idolGroupNameKr === value.idolGroupNameKr
      }
      sx={{
        width: isProfile ? "50vw" : "100%",
      }}
      noOptionsText="해당 그룹이 없습니다"
      renderOption={(props, option) => {
        const { key, ...restProps } = props;
        const { idolGroupId, idolGroupNameKr, idolGroupNameEng } = option;
        return (
          <Box
            component="li"
            key={idolGroupId}
            sx={{ "& > img": { mr: 2, flexShrink: 0 } }}
            {...restProps}
          >
            {`${idolGroupNameKr} (${idolGroupNameEng})`}
          </Box>
        );
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          variant="outlined"
          fullWidth
          placeholder="선택하세요"
          InputProps={{
            ...params.InputProps,
            startAdornment: (
              <React.Fragment>
                {params.InputProps.startAdornment}
              </React.Fragment>
            ),
          }}
        />
      )}
    />
  );
};

export default GroupDropdown2;
