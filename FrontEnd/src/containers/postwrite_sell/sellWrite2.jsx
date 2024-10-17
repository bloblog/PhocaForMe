// 게시글(판매) 생성 페이지
import React, { useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";

import GroupDropdown2 from "@/component/Dropdown/group2.jsx";
import MemberDropdown2 from "@/component/Dropdown/MemberDropdown2.jsx";

const BarterWrite = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const [isExchange, setIsExchange] = useState(true);

  const [selectedGroup, setSelectedGroup] = useState({
    value: "",
    label: "",
    avatarSrc: "",
  });

  const [selectedMember, setSelectedMember] = useState(null);

  const handleGroupChange = (group) => {
    if (group == null) {
      group = {
        value: "",
        label: "",
        avatarSrc: "",
      };
    }
    setSelectedGroup(group);
  };

  const handleMemberChange = (member) => {
    setSelectedMember(member);
  };

  return (
    <div>
      <div id="sell-dropdown">
        <div>
          <h3>그룹명</h3>
          <GroupDropdown2
            onChange={(group) => {
              handleGroupChange(group);
            }}
          />
        </div>
        <div>
          <h3>멤버</h3>
          <MemberDropdown2
            selectedGroup={selectedGroup.value}
            onChange={(member) => {
              handleMemberChange(member);
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default BarterWrite;
